/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.util.TransferDropTargetListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;

import org.eclipse.ui.views.navigator.LocalSelectionTransfer;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.reorg.JavaCopyProcessor;
import org.eclipse.jdt.internal.corext.refactoring.reorg.JavaMoveProcessor;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgDestinationFactory;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgPolicyFactory;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgUtils;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IReorgPolicy.ICopyPolicy;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IReorgPolicy.IMovePolicy;

import org.eclipse.jdt.internal.ui.dnd.JdtViewerDropAdapter;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.reorg.ReorgCopyStarter;
import org.eclipse.jdt.internal.ui.refactoring.reorg.ReorgMoveStarter;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class SelectionTransferDropAdapter extends JdtViewerDropAdapter implements TransferDropTargetListener {

	private List fElements;
	private JavaMoveProcessor fMoveProcessor;
	private int fCanMoveElements;
	private JavaCopyProcessor fCopyProcessor;
	private int fCanCopyElements;
	private ISelection fSelection;

	public SelectionTransferDropAdapter(StructuredViewer viewer) {
		super(viewer);
		
		setScrollEnabled(true);
		setExpandEnabled(true);
		setFeedbackEnabled(false);
	}

	//---- TransferDropTargetListener interface ---------------------------------------
	
	/**
	 * {@inheritDoc}
	 */
	public Transfer getTransfer() {
		return LocalSelectionTransfer.getInstance();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean isEnabled(DropTargetEvent event) {
		Object target= event.item != null ? event.item.getData() : null;
		if (target == null)
			return false;
		return target instanceof IJavaElement || target instanceof IResource;
	}

	//---- Actual DND -----------------------------------------------------------------
	
	/**
	 * {@inheritDoc}
	 */
	public void dragEnter(DropTargetEvent event) {
		clear();
		super.dragEnter(event);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void dragLeave(DropTargetEvent event) {
		clear();
		super.dragLeave(event);
	}
	
	private void clear() {
		fElements= null;
		fSelection= null;
		fMoveProcessor= null;
		fCanMoveElements= 0;
		fCopyProcessor= null;
		fCanCopyElements= 0;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean validateDrop(Object target, int operation, TransferData transferType) {
		return determineOperation(target, operation, transferType) != DND.DROP_NONE;
	}
	
	/**
	 * {@inheritDoc}
	 */
	protected int determineOperation(Object target, int operation, TransferData transferType) {
		
		initializeSelection();
		
		if (target == null)
			return DND.DROP_NONE;
		
		//Do not allow to drop on itself, bug 14228
		if (fElements.size() == 1) {
			IJavaElement[] javaElements= ReorgUtils.getJavaElements(fElements);
			IResource[] resources= ReorgUtils.getResources(fElements);
			
			if (javaElements.length == 1 && javaElements[0].equals(target))
				return DND.DROP_NONE;
			
			if (resources.length == 1 && resources[0].equals(target))
				return DND.DROP_NONE;
		}
				
		try {
			switch(operation) {
				case DND.DROP_DEFAULT:
					return handleValidateDefault(target);
				case DND.DROP_COPY: 
					return handleValidateCopy(target);
				case DND.DROP_MOVE: 	
					return handleValidateMove(target);
			}
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, PackagesMessages.SelectionTransferDropAdapter_error_title, PackagesMessages.SelectionTransferDropAdapter_error_message); 
		}
		
		return DND.DROP_NONE;
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

	/**
	 * {@inheritDoc}
	 */
	public boolean performDrop(Object data) {
		try{
			switch(getCurrentOperation()) {
				case DND.DROP_MOVE: handleDropMove(getCurrentTarget()); break;
				case DND.DROP_COPY: handleDropCopy(getCurrentTarget()); break;
			}
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, PackagesMessages.SelectionTransferDropAdapter_error_title, PackagesMessages.SelectionTransferDropAdapter_error_message); 
		} catch(InvocationTargetException e) {
			ExceptionHandler.handle(e, RefactoringMessages.OpenRefactoringWizardAction_refactoring, RefactoringMessages.OpenRefactoringWizardAction_exception); 
		} catch (InterruptedException e) {
			//ok
		}
		// The drag source listener must not perform any operation
		// since this drop adapter did the remove of the source even
		// if we moved something.
		return false;
		
	}
	
	private int handleValidateDefault(Object target) throws JavaModelException{
		int result= handleValidateMove(target);
		if (result != DND.DROP_NONE)
			return result;
			
		return handleValidateCopy(target);
	}
	
	private int handleValidateMove(Object target) throws JavaModelException{		
		if (fMoveProcessor == null) {
			IMovePolicy policy= ReorgPolicyFactory.createMovePolicy(ReorgUtils.getResources(fElements), ReorgUtils.getJavaElements(fElements));
			if (policy.canEnable())
				fMoveProcessor= new JavaMoveProcessor(policy);
		}

		if (!canMoveElements())
			return DND.DROP_NONE;	
		
		if (fMoveProcessor == null)
			return DND.DROP_NONE;

		if (!fMoveProcessor.setDestination(ReorgDestinationFactory.createDestination(target)).isOK())
			return DND.DROP_NONE;	

		return DND.DROP_MOVE;
	}
	
	private boolean canMoveElements() {
		if (fCanMoveElements == 0) {
			fCanMoveElements= 2;
			if (fMoveProcessor == null)
				fCanMoveElements= 1;
		}
		return fCanMoveElements == 2;
	}

	private void handleDropMove(final Object target) throws JavaModelException, InvocationTargetException, InterruptedException{
		IJavaElement[] javaElements= ReorgUtils.getJavaElements(fElements);
		IResource[] resources= ReorgUtils.getResources(fElements);
		ReorgMoveStarter starter= ReorgMoveStarter.create(javaElements, resources, ReorgDestinationFactory.createDestination(target));

		if (starter != null)
			starter.run(getShell());
	}

	private int handleValidateCopy(Object target) throws JavaModelException{

		if (fCopyProcessor == null) {
			final ICopyPolicy policy= ReorgPolicyFactory.createCopyPolicy(ReorgUtils.getResources(fElements), ReorgUtils.getJavaElements(fElements));
			fCopyProcessor= policy.canEnable() ? new JavaCopyProcessor(policy) : null;
		}

		if (!canCopyElements())
			return DND.DROP_NONE;
		
		if (fCopyProcessor == null)
			return DND.DROP_NONE;
		
		if (!fCopyProcessor.setDestination(ReorgDestinationFactory.createDestination(target)).isOK())
			return DND.DROP_NONE;
			
		return DND.DROP_COPY;					
	}
			
	private boolean canCopyElements() {
		if (fCanCopyElements == 0) {
			fCanCopyElements= 2;
			if (fCopyProcessor == null)
				fCanCopyElements= 1;
		}
		return fCanCopyElements == 2;
	}		
	
	private void handleDropCopy(final Object target) throws JavaModelException, InvocationTargetException, InterruptedException{
		IJavaElement[] javaElements= ReorgUtils.getJavaElements(fElements);
		IResource[] resources= ReorgUtils.getResources(fElements);
		ReorgCopyStarter starter= ReorgCopyStarter.create(javaElements, resources, ReorgDestinationFactory.createDestination(target));
		
		if (starter != null)
			starter.run(getShell());
	}

	private Shell getShell() {
		return getViewer().getControl().getShell();
	}
}
