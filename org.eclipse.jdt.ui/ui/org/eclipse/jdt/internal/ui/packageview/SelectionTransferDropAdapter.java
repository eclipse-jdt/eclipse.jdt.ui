/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.packageview;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.resources.IResource;

import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.actions.AddMethodStubAction;
import org.eclipse.jdt.internal.ui.dnd.JdtViewerDropAdapter;
import org.eclipse.jdt.internal.ui.dnd.LocalSelectionTransfer;
import org.eclipse.jdt.internal.ui.dnd.TransferDropTargetListener;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.reorg.ReorgCopyStarter;
import org.eclipse.jdt.internal.ui.refactoring.reorg.ReorgMoveStarter;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

import org.eclipse.jdt.internal.corext.refactoring.reorg.CopyRefactoring2;
import org.eclipse.jdt.internal.corext.refactoring.reorg.MoveRefactoring2;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgUtils2;

public class SelectionTransferDropAdapter extends JdtViewerDropAdapter implements TransferDropTargetListener {

	private List fElements;
	private MoveRefactoring2 fMoveRefactoring2;
	private int fCanMoveElements;
	private CopyRefactoring2 fCopyRefactoring2;
	private int fCanCopyElements;
	private ISelection fSelection;
	private AddMethodStubAction fAddMethodStubAction;
	
	private static final int DROP_TIME_DIFF_TRESHOLD= 150;

	public SelectionTransferDropAdapter(StructuredViewer viewer) {
		super(viewer, DND.FEEDBACK_SCROLL | DND.FEEDBACK_EXPAND);
		fAddMethodStubAction= new AddMethodStubAction();
	}

	//---- TransferDropTargetListener interface ---------------------------------------
	
	public Transfer getTransfer() {
		return LocalSelectionTransfer.getInstance();
	}

	//---- Actual DND -----------------------------------------------------------------
	
	public void dragEnter(DropTargetEvent event) {
		clear();
		super.dragEnter(event);
	}
	
	public void dragLeave(DropTargetEvent event) {
		clear();
		super.dragLeave(event);
	}
	
	private void clear() {
		fElements= null;
		fSelection= null;
		fMoveRefactoring2= null;
		fCanMoveElements= 0;
		fCopyRefactoring2= null;
		fCanCopyElements= 0;
	}
	
	public void validateDrop(Object target, DropTargetEvent event, int operation) {
		event.detail= DND.DROP_NONE;
		
		if (tooFast(event)) 
			return;
		
		initializeSelection();
				
		try {
			switch(operation) {
				case DND.DROP_DEFAULT:	event.detail= handleValidateDefault(target, event); break;
				case DND.DROP_COPY: 		event.detail= handleValidateCopy(target, event); break;
				case DND.DROP_MOVE: 		event.detail= handleValidateMove(target, event); break;
				case DND.DROP_LINK: 			event.detail= handleValidateLink(target, event); break;
			}
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, PackagesMessages.getString("SelectionTransferDropAdapter.error.title"), PackagesMessages.getString("SelectionTransferDropAdapter.error.message")); //$NON-NLS-1$ //$NON-NLS-2$
			event.detail= DND.DROP_NONE;
		}	
	}

	protected void initializeSelection(){
		if (fElements != null)
			return;
		ISelection s= LocalSelectionTransfer.getInstance().getSelection();
		if (!(s instanceof IStructuredSelection))
			return;
		fSelection= s;	
		fElements= ((IStructuredSelection)s).toList();
	}
	
	protected ISelection getSelection(){
		return fSelection;
	}
	
	private boolean tooFast(DropTargetEvent event) {
		return Math.abs(LocalSelectionTransfer.getInstance().getSelectionSetTime() - event.time) < DROP_TIME_DIFF_TRESHOLD;
	}	

	public void drop(Object target, DropTargetEvent event) {
		try{
			switch(event.detail) {
				case DND.DROP_MOVE: handleDropMove(target, event); break;
				case DND.DROP_COPY: handleDropCopy(target, event); break;
				case DND.DROP_LINK: handleDropLink(target, event); break;
			}
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, PackagesMessages.getString("SelectionTransferDropAdapter.error.title"), PackagesMessages.getString("SelectionTransferDropAdapter.error.message")); //$NON-NLS-1$ //$NON-NLS-2$
		} catch(InvocationTargetException e) {
			ExceptionHandler.handle(e, RefactoringMessages.getString("OpenRefactoringWizardAction.refactoring"), RefactoringMessages.getString("OpenRefactoringWizardAction.exception")); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (InterruptedException e) {
			//ok
		}	finally{
			// The drag source listener must not perform any operation
			// since this drop adapter did the remove of the source even
			// if we moved something.
			event.detail= DND.DROP_NONE;
		}
	}
	
	private int handleValidateDefault(Object target, DropTargetEvent event) throws JavaModelException{
		if (target == null)
			return DND.DROP_NONE;
			
		return handleValidateMove(target, event);	
	}
	
	private int handleValidateMove(Object target, DropTargetEvent event) throws JavaModelException{
		if (target == null)
			return DND.DROP_NONE;
		
		if (fMoveRefactoring2 == null) {
			IResource[] resources= ReorgUtils2.getResources(fElements);
			IJavaElement[] javaElements= ReorgUtils2.getJavaElements(fElements);
			fMoveRefactoring2= MoveRefactoring2.create(resources, javaElements, JavaPreferencesSettings.getCodeGenerationSettings());
		}
		
		if (!canMoveElements())
			return DND.DROP_NONE;	

		if (target instanceof IResource && fMoveRefactoring2 != null && fMoveRefactoring2.setDestination((IResource)target).isOK())
			return DND.DROP_MOVE;
		else if (target instanceof IJavaElement && fMoveRefactoring2 != null && fMoveRefactoring2.setDestination((IJavaElement)target).isOK())
			return DND.DROP_MOVE;
		else
			return DND.DROP_NONE;	
	}
	
	private boolean canMoveElements() {
		if (fCanMoveElements == 0) {
			fCanMoveElements= 2;
			if (fMoveRefactoring2 == null)
				fCanMoveElements= 1;
		}
		return fCanMoveElements == 2;
	}
	
	private void handleDropLink(Object target, DropTargetEvent event) {
		if (fAddMethodStubAction.init((IType)target, getSelection())) 
			fAddMethodStubAction.run();
	}

	private int handleValidateLink(Object target, DropTargetEvent event) {
		if (target instanceof IType && AddMethodStubAction.canActionBeAdded((IType)target, getSelection()))
			return DND.DROP_LINK;
		else		
			return DND.DROP_NONE;
	}
	
	private void handleDropMove(final Object target, DropTargetEvent event) throws JavaModelException, InvocationTargetException, InterruptedException{
		IJavaElement[] javaElements= ReorgUtils2.getJavaElements(fElements);
		IResource[] resources= ReorgUtils2.getResources(fElements);
		ReorgMoveStarter starter= null;
		if (target instanceof IResource) 
			starter= ReorgMoveStarter.create(javaElements, resources, (IResource)target);
		else if (target instanceof IJavaElement)
			starter= ReorgMoveStarter.create(javaElements, resources, (IJavaElement)target);
		if (starter != null)
			starter.run(getShell());
	}

	private int handleValidateCopy(Object target, DropTargetEvent event) throws JavaModelException{

		if (fCopyRefactoring2 == null) {
			IResource[] resources= ReorgUtils2.getResources(fElements);
			IJavaElement[] javaElements= ReorgUtils2.getJavaElements(fElements);
			fCopyRefactoring2= CopyRefactoring2.create(resources, javaElements, JavaPreferencesSettings.getCodeGenerationSettings());
		}
		
		if (!canCopyElements())
			return DND.DROP_NONE;	

		if (target instanceof IResource && fCopyRefactoring2 != null && fCopyRefactoring2.setDestination((IResource)target).isOK())
			return DND.DROP_COPY;
		else if (target instanceof IJavaElement && fCopyRefactoring2 != null && fCopyRefactoring2.setDestination((IJavaElement)target).isOK())
			return DND.DROP_COPY;
		else
			return DND.DROP_NONE;					
	}
			
	private boolean canCopyElements() {
		if (fCanCopyElements == 0) {
			fCanCopyElements= 2;
			if (fCopyRefactoring2 == null)
				fCanCopyElements= 1;
		}
		return fCanCopyElements == 2;
	}		
	
	private void handleDropCopy(final Object target, DropTargetEvent event) throws JavaModelException, InvocationTargetException, InterruptedException{
		IJavaElement[] javaElements= ReorgUtils2.getJavaElements(fElements);
		IResource[] resources= ReorgUtils2.getResources(fElements);
		ReorgCopyStarter starter= null;
		if (target instanceof IResource) 
			starter= ReorgCopyStarter.create(javaElements, resources, (IResource)target);
		else if (target instanceof IJavaElement)
			starter= ReorgCopyStarter.create(javaElements, resources, (IJavaElement)target);
		if (starter != null)
			starter.run(getShell());
	}

	private Shell getShell() {
		return getViewer().getControl().getShell();
	}
}
