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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.AddMethodStubAction;
import org.eclipse.jdt.internal.ui.dnd.JdtViewerDropAdapter;
import org.eclipse.jdt.internal.ui.dnd.LocalSelectionTransfer;
import org.eclipse.jdt.internal.ui.dnd.TransferDropTargetListener;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.QualifiedNameComponent;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizardPage;
import org.eclipse.jdt.internal.ui.reorg.JdtCopyAction;
import org.eclipse.jdt.internal.ui.reorg.JdtMoveAction;
import org.eclipse.jdt.internal.ui.reorg.MockWorkbenchSite;
import org.eclipse.jdt.internal.ui.reorg.ReorgActionFactory;
import org.eclipse.jdt.internal.ui.reorg.ReorgMessages;
import org.eclipse.jdt.internal.ui.reorg.ReorgQueries;
import org.eclipse.jdt.internal.ui.reorg.SimpleSelectionProvider;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.reorg.CopyRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IPackageFragmentRootManipulationQuery;
import org.eclipse.jdt.internal.corext.refactoring.reorg.MoveRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.reorg.SourceReferenceUtil;
import org.eclipse.jdt.internal.corext.refactoring.reorg2.DeleteAction;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;

public class SelectionTransferDropAdapter extends JdtViewerDropAdapter implements TransferDropTargetListener {

	private List fElements;
	private MoveRefactoring fMoveRefactoring;
	private int fCanMoveElements;
	private CopyRefactoring fCopyRefactoring;
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
		fMoveRefactoring= null;
		fCanMoveElements= 0;
		fCopyRefactoring= null;
		fCanCopyElements= 0;
	}
	
	public void validateDrop(Object target, DropTargetEvent event, int operation) {
		event.detail= DND.DROP_NONE;
		
		if (tooFast(event)) 
			return;
		
		initializeSelection();
				
		try {
			if (operation == DND.DROP_DEFAULT) {
				event.detail= handleValidateDefault(target, event);
			} else if (operation == DND.DROP_COPY) {
				event.detail= handleValidateCopy(target, event);
			} else if (operation == DND.DROP_MOVE) {
				event.detail= handleValidateMove(target, event);
			} else if (operation == DND.DROP_LINK) {
				event.detail= handleValidateLink(target, event);
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
			if (event.detail == DND.DROP_MOVE) {
				handleDropMove(target, event);
				
				if (! canPasteSourceReferences(target))
					return;
				DeleteAction delete= ReorgActionFactory.createDeleteAction(getDragableSourceReferences());
				delete.setSuggestGetterSetterDeletion(false);
				if (delete.isEnabled())
					delete.run();
				
			} else if (event.detail == DND.DROP_COPY) {
				handleDropCopy(target, event);
			} else if (event.detail == DND.DROP_LINK) {
				handleDropLink(target, event);
			}
			
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, PackagesMessages.getString("SelectionTransferDropAdapter.error.title"), PackagesMessages.getString("SelectionTransferDropAdapter.error.message")); //$NON-NLS-1$ //$NON-NLS-2$
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
			
		if (canPasteSourceReferences(target))	
			return handleValidateCopy(target, event);
		else
			return handleValidateMove(target, event);	
	}
	
	private int handleValidateMove(Object target, DropTargetEvent event) throws JavaModelException{
		if (target == null)
			return DND.DROP_NONE;
		
		if (canPasteSourceReferences(target)){
			if (canMoveSelectedSourceReferences(target))
				return DND.DROP_MOVE;
			else	
				return DND.DROP_NONE;
		}	
		
		if (fMoveRefactoring == null){
			IPackageFragmentRootManipulationQuery query= JdtMoveAction.createUpdateClasspathQuery(getViewer().getControl().getShell());
			fMoveRefactoring= MoveRefactoring.create(fElements, JavaPreferencesSettings.getCodeGenerationSettings(), query);
		}	
		
		if (!canMoveElements())
			return DND.DROP_NONE;	
		
		if (fMoveRefactoring != null && fMoveRefactoring.isValidDestination(target))
			return DND.DROP_MOVE;
		else
			return DND.DROP_NONE;	
	}
	
	private boolean canMoveElements() {
		if (fCanMoveElements == 0) {
			fCanMoveElements= 2;
			if (fMoveRefactoring == null)
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
	
	private void handleDropMove(final Object target, DropTargetEvent event) throws JavaModelException{
		if (canPasteSourceReferences(target)){
			pasteSourceReferences(target, event);
			return;
		}
		new DragNDropMoveAction(new SimpleSelectionProvider(fElements), target).run();
	}

	private void pasteSourceReferences(final Object target, DropTargetEvent event) {
		SelectionDispatchAction pasteAction= ReorgActionFactory.createPasteAction(getDragableSourceReferences(), target);
		pasteAction.update(pasteAction.getSelection());
		if (!pasteAction.isEnabled()){
			event.detail= DND.DROP_NONE;
			return;
		}	
		pasteAction.run();	
		
		return;
	}
	
	private int handleValidateCopy(Object target, DropTargetEvent event) throws JavaModelException{

		if (canPasteSourceReferences(target))
			return DND.DROP_COPY;
		
		if (fCopyRefactoring == null){
			IPackageFragmentRootManipulationQuery query= JdtCopyAction.createUpdateClasspathQuery(getViewer().getControl().getShell());
			fCopyRefactoring= CopyRefactoring.create(fElements, new ReorgQueries(), query);
		}
		
		if (!canCopyElements())
			return DND.DROP_NONE;	

		if (fCopyRefactoring != null && fCopyRefactoring.isValidDestination(target))
			return DND.DROP_COPY;
		else
			return DND.DROP_NONE;					
	}

	private boolean canMoveSelectedSourceReferences(Object target) throws JavaModelException{
		ICompilationUnit targetCu= getCompilationUnit(target);
		if (targetCu == null)
			return false;
			
		ISourceReference[] elements= getDragableSourceReferences();		
		for (int i= 0; i < elements.length; i++) {
			if (targetCu.equals(SourceReferenceUtil.getCompilationUnit(elements[i])))	
				return false;
		}
		return true;
	}
	
	private static ICompilationUnit getCompilationUnit(Object target){
		if (target instanceof ISourceReference)
			return SourceReferenceUtil.getCompilationUnit((ISourceReference)target);
		else
			return null;	
	}
	
	private boolean canPasteSourceReferences(Object target) throws JavaModelException{
		ISourceReference[] elements= getDragableSourceReferences();
		if (elements.length != fElements.size())
			return false;
		SelectionDispatchAction pasteAction= ReorgActionFactory.createPasteAction(elements, target);
		pasteAction.update(pasteAction.getSelection());
		return pasteAction.isEnabled();
	}
	
	private ISourceReference[] getDragableSourceReferences(){
		List result= new ArrayList(fElements.size());
		for(Iterator iter= fElements.iterator(); iter.hasNext();){
			Object each= iter.next();
			if (isDragableSourceReferences(each))
				result.add(each);
		}
		return (ISourceReference[])result.toArray(new ISourceReference[result.size()]);
	}
	
	private static boolean isDragableSourceReferences(Object element) {
		if (!(element instanceof ISourceReference))
			return false;
		if (!(element instanceof IJavaElement))
			return false;
		if (element instanceof ICompilationUnit)
			return false;
		
		return true;	
	}
			
	private boolean canCopyElements() {
		if (fCanCopyElements == 0) {
			fCanCopyElements= 2;
			if (fCopyRefactoring == null)
				fCanCopyElements= 1;
		}
		return fCanCopyElements == 2;
	}		
	
	private void handleDropCopy(final Object target, DropTargetEvent event) throws JavaModelException{
		if (canPasteSourceReferences(target)){
			pasteSourceReferences(target, event);
			return;
		}
		
		SelectionDispatchAction action= ReorgActionFactory.createDnDCopyAction(fElements, ResourceUtil.getResource(target));
		action.run();
	}
	
	//--
	private static class DragNDropMoveAction extends JdtMoveAction{
		private Object fTarget;
		
		public DragNDropMoveAction(ISelectionProvider provider, Object target){
			super(new MockWorkbenchSite(provider));
			Assert.isNotNull(target);
			fTarget= target;
		}
		
		protected Object selectDestination(ReorgRefactoring ref) {
			return fTarget;
		}
		
		protected boolean isOkToProceed(ReorgRefactoring refactoring) throws JavaModelException {
			if (!super.isOkToProceed(refactoring))
				return false;
			return askIfUpdateReferences((MoveRefactoring)refactoring);
		}
		
		//returns false iff canceled or error
		private boolean askIfUpdateReferences(MoveRefactoring ref) throws JavaModelException {
			if (! ref.canUpdateReferences() && !ref.canUpdateQualifiedNames()) {
				setShowPreview(false);
				return true;
			}	
			switch (showMoveDialog(ref)){
				case IDialogConstants.CANCEL_ID:
				 	setShowPreview(false);
					return false;
				case IDialogConstants.OK_ID:
					setShowPreview(false);
					return true;
				case PREVIEW_ID:		 
					setShowPreview(true);
					return true;
				default: 
					Assert.isTrue(false); //not expected to get here
					return false;
			}
		}
		
		private static int showMoveDialog(MoveRefactoring ref) {
			Shell shell= JavaPlugin.getActiveWorkbenchShell().getShell();
			final UpdateDialog dialog= new UpdateDialog(shell, ref);
			shell.getDisplay().syncExec(new Runnable() {
				public void run() {
					dialog.open();
				}
			});
			return dialog.getReturnCode();
		}
		
		private static class UpdateDialog extends Dialog {
			private Button fPreview;
			private MoveRefactoring fRefactoring;
			private Button fReferenceCheckbox;
			private Button fQualifiedNameCheckbox;
			private QualifiedNameComponent fQualifiedNameComponent;
			
			public UpdateDialog(Shell parentShell, MoveRefactoring refactoring) {
				super(parentShell);
				fRefactoring= refactoring;
			}
			protected void configureShell(Shell shell) {
				shell.setText(PackagesMessages.getString("SelectionTransferDropAdapter.dialog.title")); //$NON-NLS-1$
				super.configureShell(shell);
			}
			protected void createButtonsForButtonBar(Composite parent) {
				fPreview= createButton(parent, PREVIEW_ID, ReorgMessages.getString("JdtMoveAction.preview"), false); //$NON-NLS-1$
				super.createButtonsForButtonBar(parent);
			}
			protected void buttonPressed(int buttonId) {
				if (buttonId == PREVIEW_ID) {
					setReturnCode(PREVIEW_ID);
					close();
				} else {
					super.buttonPressed(buttonId);
				}
			}

			protected Control createDialogArea(Composite parent) {
				Composite result= (Composite)super.createDialogArea(parent);
				addUpdateReferenceComponent(result);
				addUpdateQualifiedNameComponent(result, ((GridLayout)result.getLayout()).marginWidth);
				applyDialogFont(result);		
				return result;
			}
			private void updateButtons() {
				Button okButton= getButton(IDialogConstants.OK_ID);
				boolean okEnabled= true;	// we keep this since the code got copied from JdtMoveAction. 
				okButton.setEnabled(okEnabled);
				fReferenceCheckbox.setEnabled(okEnabled && canUpdateReferences());
				fRefactoring.setUpdateReferences(fReferenceCheckbox.getEnabled() && fReferenceCheckbox.getSelection());
				if (fQualifiedNameCheckbox != null) {
					boolean enabled= okEnabled && fRefactoring.canEnableQualifiedNameUpdating();
					fQualifiedNameCheckbox.setEnabled(enabled);
					if (enabled) {
						fQualifiedNameComponent.setEnabled(fRefactoring.getUpdateQualifiedNames());
						if (fRefactoring.getUpdateQualifiedNames())
							okButton.setEnabled(false);
					} else {
						fQualifiedNameComponent.setEnabled(false);
					}
					fRefactoring.setUpdateQualifiedNames(fQualifiedNameCheckbox.getEnabled() && fQualifiedNameCheckbox.getSelection());
				}
				boolean preview= okEnabled;
				if (preview)
					preview= 
						fRefactoring.getUpdateQualifiedNames() && fRefactoring.canEnableQualifiedNameUpdating() ||
						fReferenceCheckbox.getSelection() && canUpdateReferences();
				fPreview.setEnabled(preview);
			}
			private void addUpdateReferenceComponent(Composite result) {
				fReferenceCheckbox= new Button(result, SWT.CHECK);
				fReferenceCheckbox.setText(ReorgMessages.getString("JdtMoveAction.update_references")); //$NON-NLS-1$
				fReferenceCheckbox.setSelection(fRefactoring.getUpdateReferences());
				fReferenceCheckbox.setEnabled(canUpdateReferences());
			
				fReferenceCheckbox.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						fRefactoring.setUpdateReferences(((Button)e.widget).getSelection());
						updateButtons();
					}
				});
			}
			private boolean canUpdateReferences() {
				try{
					return fRefactoring.canUpdateReferences();
				} catch (JavaModelException e){
					return false;
				}
			}
			private void addUpdateQualifiedNameComponent(Composite parent, int marginWidth) {
				if (!fRefactoring.canUpdateQualifiedNames())
					return;
				fQualifiedNameCheckbox= new Button(parent, SWT.CHECK);
				int indent= marginWidth + fQualifiedNameCheckbox.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
				fQualifiedNameCheckbox.setText(RefactoringMessages.getString("RenameInputWizardPage.update_qualified_names")); //$NON-NLS-1$
				fQualifiedNameCheckbox.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				fQualifiedNameCheckbox.setSelection(fRefactoring.getUpdateQualifiedNames());
		
				fQualifiedNameComponent= new QualifiedNameComponent(parent, SWT.NONE, fRefactoring, getRefactoringSettings());
				fQualifiedNameComponent.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				GridData gd= (GridData)fQualifiedNameComponent.getLayoutData();
				gd.horizontalAlignment= GridData.FILL;
				gd.horizontalIndent= indent;
				fQualifiedNameComponent.setEnabled(false);

				fQualifiedNameCheckbox.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						boolean enabled= ((Button)e.widget).getSelection();
						fQualifiedNameComponent.setEnabled(enabled);
						fRefactoring.setUpdateQualifiedNames(enabled);
						updateButtons();
					}
				});
			}
			protected IDialogSettings getRefactoringSettings() {
				IDialogSettings settings= JavaPlugin.getDefault().getDialogSettings();
				if (settings == null)
					return null;
				IDialogSettings result= settings.getSection(RefactoringWizardPage.REFACTORING_SETTINGS);
				if (result == null) {
					result= new DialogSettings(RefactoringWizardPage.REFACTORING_SETTINGS);
					settings.addSection(result); 
				}
				return result;
			}
			public boolean close() {
				if (getReturnCode() != IDialogConstants.CANCEL_ID && fQualifiedNameComponent != null) {
					fQualifiedNameComponent.savePatterns(getRefactoringSettings());
				}
				return super.close();
			}
		}
	}
}
