package org.eclipse.jdt.internal.ui.reorg;


import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.MoveProjectAction;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.StandardJavaElementContentProvider;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.reorg.MoveRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgUtils;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.CheckConditionsOperation;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizardDialog;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringErrorDialogUtil;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class JdtMoveAction extends ReorgDestinationAction {

	private boolean fShowPreview= false;

	public JdtMoveAction(IWorkbenchSite site) {
		super(site);
		setText(ReorgMessages.getString("moveAction.label"));//$NON-NLS-1$
	}

	public boolean canOperateOn(IStructuredSelection selection) {
		if (ClipboardActionUtil.hasOnlyProjects(selection))
			return selection.size() == 1;
		else
			return super.canOperateOn(selection);
	}
	
	protected void run(IStructuredSelection selection) {
		if (ClipboardActionUtil.hasOnlyProjects(selection)){
			moveProject(selection);
		}	else {
			super.run(selection);
		}
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
		return new MoveRefactoring(elements, JavaPreferencesSettings.getCodeGenerationSettings());
	}
	
	ElementTreeSelectionDialog createDestinationSelectionDialog(Shell parent, ILabelProvider labelProvider, StandardJavaElementContentProvider cp, ReorgRefactoring refactoring){
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
		if (! hasReadOnlyElements(refactoring))
			return true;
		
		return  MessageDialog.openQuestion(
				JavaPlugin.getActiveWorkbenchShell(),
				ReorgMessages.getString("moveAction.checkMove"),  //$NON-NLS-1$
				ReorgMessages.getString("moveAction.error.readOnly")); //$NON-NLS-1$
	}
	
	private static boolean hasReadOnlyElements(ReorgRefactoring refactoring){
		for (Iterator iter= refactoring.getElementsToReorg().iterator(); iter.hasNext(); ){
			if (ReorgUtils.shouldConfirmReadOnly(iter.next())) 
				return true;
		}
		return false;
	}
		
	/* non java-doc
	 * @see ReorgDestinationAction#doReorg(ReorgRefactoring) 
	 */
	void doReorg(ReorgRefactoring refactoring) throws JavaModelException{
		if (fShowPreview){		
			//XX incorrect help
			RefactoringWizard wizard= new RefactoringWizard(refactoring, ReorgMessages.getString("JdtMoveAction.move"), IJavaHelpContextIds.MOVE_CU_ERROR_WIZARD_PAGE); //$NON-NLS-1$
	        wizard.setChangeCreationCancelable(false);
			new RefactoringWizardDialog(JavaPlugin.getActiveWorkbenchShell(), wizard).open();	
			return;	
		}
	
        CheckConditionsOperation runnable= new CheckConditionsOperation(refactoring, CheckConditionsOperation.PRECONDITIONS);
        try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().run(false, false, runnable);
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, getShell(), ReorgMessages.getString("JdtMoveAction.move"), ReorgMessages.getString("JdtMoveAction.exception")); //$NON-NLS-1$ //$NON-NLS-2$
			return;
		} catch (InterruptedException e) {
			Assert.isTrue(false); //cannot happen - not cancelable
		}
		RefactoringStatus status= runnable.getStatus();           
		if (status == null)
			return;
		if (status.hasFatalError())
			RefactoringErrorDialogUtil.open(ReorgMessages.getString("JdtMoveAction.move"), status);//$NON-NLS-1$
		else
			super.doReorg(refactoring);
	}
	
	private void moveProject(IStructuredSelection selection){
		MoveProjectAction action= new MoveProjectAction(JavaPlugin.getActiveWorkbenchShell());
		action.selectionChanged(selection);
		action.run();
	}
	
	//--- static inner classes
		
	private class MoveDestinationDialog extends ElementTreeSelectionDialog {
		private static final int PREVIEW_ID= IDialogConstants.CLIENT_ID + 1;
		private MoveRefactoring fRefactoring;
		private Button fCheckbox;
		private Button fPreview;
		
		MoveDestinationDialog(Shell parent, ILabelProvider labelProvider, 	ITreeContentProvider contentProvider, MoveRefactoring refactoring){
			super(parent, labelProvider, contentProvider);
			fRefactoring= refactoring;
			fShowPreview= false;//from outter instance
			setDoubleClickSelects(false);
		}
		
		protected Control createDialogArea(Composite parent) {
			Composite result= (Composite)super.createDialogArea(parent);
			fCheckbox= new Button(result, SWT.CHECK);
			fCheckbox.setText(ReorgMessages.getString("JdtMoveAction.update_references")); //$NON-NLS-1$
			fCheckbox.setEnabled(canUpdateReferences());
			fCheckbox.setSelection(true);
			
			fCheckbox.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					updatePreviewButton();
					fRefactoring.setUpdateReferences(getUpdateReferences());
				}
			});
			return result;
		}
		
		protected void createButtonsForButtonBar(Composite parent) {
			super.createButtonsForButtonBar(parent);
			fPreview= createButton(parent, PREVIEW_ID, ReorgMessages.getString("JdtMoveAction.preview"), false); //$NON-NLS-1$
		}
		
		protected void updateOKStatus() {
			super.updateOKStatus();
			try{
				fRefactoring.setDestination(getFirstResult());
				fCheckbox.setEnabled(getOkButton().getEnabled() &&  canUpdateReferences());
				fRefactoring.setUpdateReferences(getUpdateReferences());
				updatePreviewButton();
			} catch (JavaModelException e){
				ExceptionHandler.handle(e, ReorgMessages.getString("JdtMoveAction.move"), ReorgMessages.getString("JdtMoveAction.exception")); //$NON-NLS-1$ //$NON-NLS-2$
			}		
		}

		private boolean getUpdateReferences() {
			return fCheckbox.getEnabled() && fCheckbox.getSelection();
		}
		
		protected void buttonPressed(int buttonId) {
			fShowPreview= (buttonId == PREVIEW_ID);
			super.buttonPressed(buttonId);
			if (buttonId == PREVIEW_ID)
				close();
		}
		
		private void updatePreviewButton(){
			fPreview.setEnabled(getUpdateReferences());
		}
		
		private boolean canUpdateReferences(){
			try{
				return fRefactoring.canUpdateReferences();
			} catch (JavaModelException e){
				return false;
			}
		}
		/*
		 * @see org.eclipse.jface.window.Window#configureShell(Shell)
		 */
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			WorkbenchHelp.setHelp(newShell, IJavaHelpContextIds.MOVE_DESTINATION_DIALOG);
		}

	}
}
