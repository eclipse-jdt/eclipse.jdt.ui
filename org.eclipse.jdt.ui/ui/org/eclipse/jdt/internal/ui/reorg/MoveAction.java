/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.reorg;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;
import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.core.refactoring.reorg.MoveRefactoring;
import org.eclipse.jdt.internal.core.refactoring.reorg.ReorgRefactoring;
import org.eclipse.jdt.internal.core.refactoring.reorg.ReorgUtils;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.jdt.internal.ui.refactoring.CreateChangeOperation;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizardDialog;
import org.eclipse.jdt.internal.ui.refactoring.actions.StructuredSelectionProvider;
import org.eclipse.jdt.internal.ui.refactoring.changes.DocumentTextBufferChangeCreator;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.ui.JavaElementContentProvider;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.texteditor.IDocumentProvider;

public class MoveAction extends ReorgDestinationAction {
	
	private boolean fShowPreview= false;
	
	public MoveAction(StructuredSelectionProvider provider) {
		this(ReorgMessages.getString("moveAction.label"), provider); //$NON-NLS-1$
	}
	
	public MoveAction(String name, StructuredSelectionProvider provider) {
		super(name, provider);
		setDescription(ReorgMessages.getString("moveAction.description")); //$NON-NLS-1$
	}

	/* non java-doc
	 * see @ReorgDestinationAction#isOkToProceed
	 */
	String getActionName() {
		return ReorgMessages.getString("moveAction.name"); //$NON-NLS-1$
	}
	
	/* non java-doc
	 * see @ReorgDestinationAction#getDestinationDialogMessage
	 */
	String getDestinationDialogMessage() {
		return ReorgMessages.getString("moveAction.destination.label"); //$NON-NLS-1$
	}
	
	/* non java-doc
	 * see @ReorgDestinationAction#createRefactoring
	 */
	ReorgRefactoring createRefactoring(List elements){
		IDocumentProvider documentProvider= JavaPlugin.getDefault().getCompilationUnitDocumentProvider();
		ITextBufferChangeCreator changeCreator= new DocumentTextBufferChangeCreator(documentProvider);
		return new MoveRefactoring(elements, changeCreator);
	}
	
	ElementTreeSelectionDialog createDestinationSelectionDialog(Shell parent, ILabelProvider labelProvider, JavaElementContentProvider cp, ReorgRefactoring refactoring){
		return new MoveDestinationDialog(parent, labelProvider, cp, (MoveRefactoring)refactoring);
	}	
	
	/* non java-doc
	 * see @ReorgDestinationAction#isOkToProceed
	 */
	protected boolean isOkToProceed(ReorgRefactoring refactoring) throws JavaModelException{
		return (isOkToMoveReadOnly(refactoring));
	}
	
	protected void setShowPreview(boolean showPreview) {
		fShowPreview = showPreview;
	}
	
	private static boolean isOkToMoveReadOnly(ReorgRefactoring refactoring){
		for (Iterator iter= refactoring.getElementsToReorg().iterator(); iter.hasNext(); ){
			Object element= iter.next();
			if (ReorgUtils.shouldConfirmReadOnly(element)) {
				return  MessageDialog.openQuestion(
					JavaPlugin.getActiveWorkbenchShell(),
					ReorgMessages.getString("moveAction.checkMove"),  //$NON-NLS-1$
					ReorgMessages.getString("moveAction.error.readOnly")); //$NON-NLS-1$
			}
		}
		return true;
	}
	
	/* non java-doc
	 * @see ReorgDestinationAction#doReorg(ReorgRefactoring) 
	 */
	void doReorg(ReorgRefactoring refactoring) throws JavaModelException{
		if (!fShowPreview){
			super.doReorg(refactoring);
			return;
		}	
		new RefactoringWizardDialog(JavaPlugin.getActiveWorkbenchShell(), new MoveWizard((MoveRefactoring)refactoring)).open();	
	}
	
	//--- static inner classes
		
	private static class MoveWizard extends RefactoringWizard{
		MoveWizard(MoveRefactoring ref){
			//XX incorrect help
			super(ref, "Move", IJavaHelpContextIds.MOVE_CU_ERROR_WIZARD_PAGE); 
		}
		
		/* (non-Javadoc)
		 * Method overridden from RefactoringWizard
		 */
		public void addPages() {
			addPreviewPage();	
			setupPageTitles();
		}
		
		public void createPageControls(Composite pageContainer) {
			super.createPageControls(pageContainer);
			
			//XX nasty to have to do it here
			setChange();
		}
		
		private void setChange(){
			IChange change= null;
			try{
				change= computeChange(getRefactoring());
			} catch (CoreException e){
				JavaPlugin.log(e.getStatus());
				return;
			}	
			if (change == null)
				return;
			setChange(change);
		}
		
		private IChange computeChange(Refactoring ref) throws CoreException{
			CreateChangeOperation op= new CreateChangeOperation(ref, CreateChangeOperation.CHECK_NONE);
			try{
				new ProgressMonitorDialog(JavaPlugin.getActiveWorkbenchShell()).run(true, true, op);
			} catch (InvocationTargetException e) {
				Throwable t= e.getTargetException();
				if (t instanceof CoreException)
					throw (CoreException)t;
				if (t instanceof Error)
					throw (Error)t;
				if (t instanceof RuntimeException)
					throw (RuntimeException)t;
				Assert.isTrue(false);//should never get here
				return null;
			} catch (InterruptedException e) {
				//fall thru
			}
			return op.getChange();
		}		
	}
	
	private class MoveDestinationDialog extends ElementTreeSelectionDialog {
		private static final int PREVIEW_ID= IDialogConstants.CLIENT_ID + 1;
		private MoveRefactoring fRefactoring;
		private Button fCheckbox;
		private Button fPreview;
		
		MoveDestinationDialog(Shell parent, ILabelProvider labelProvider, 	ITreeContentProvider contentProvider, MoveRefactoring refactoring){
			super(parent, labelProvider, contentProvider);
			fRefactoring= refactoring;
			fShowPreview= false;//from outter instance
		}
		
		protected Control createDialogArea(Composite parent) {
			Composite result= (Composite)super.createDialogArea(parent);
			fCheckbox= new Button(result, SWT.CHECK);
			fCheckbox.setText("Update references to the moved element(s).");
			fCheckbox.setEnabled(canUpdateReferences());
			fCheckbox.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					updatePreviewButton();
					fRefactoring.setUpdateReferences(fCheckbox.getEnabled() && fCheckbox.getSelection());
				}
			});
			fCheckbox.setSelection(canUpdateReferences());
			return result;
		}
		
		protected void createButtonsForButtonBar(Composite parent) {
			super.createButtonsForButtonBar(parent);
			fPreview= createButton(parent, PREVIEW_ID, "Preview", false);
		}
		
		protected void updateOKStatus() {
			super.updateOKStatus();
			try{
				fRefactoring.setDestination(getFirstResult());
				fCheckbox.setEnabled(getOkButton().getEnabled() &&  canUpdateReferences());
				updatePreviewButton();
			} catch (JavaModelException e){
				ExceptionHandler.handle(e, "Move", "Unexpected exception occurred. See log for details.");
			}		
		}
		
		protected void buttonPressed(int buttonId) {
			fShowPreview= (buttonId == PREVIEW_ID);
			super.buttonPressed(buttonId);
			if (buttonId == PREVIEW_ID)
				close();
		}
		
		private void updatePreviewButton(){
			fPreview.setEnabled(fCheckbox.getEnabled() && fCheckbox.getSelection());
		}
		
		private boolean canUpdateReferences(){
			try{
				return fRefactoring.canUpdateReferences();
			} catch (JavaModelException e){
				return false;
			}
		}
	}


}
