/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringStatusContentProvider;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringStatusEntryLabelProvider;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizardDialog;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.IStructuredSelection;

public abstract class RefactoringAction extends Action {

	private StructuredSelectionProvider fProvider;

	protected RefactoringAction(String text, StructuredSelectionProvider provider) {
		super(text);
		fProvider= provider;
		Assert.isNotNull(fProvider);
	}
	
	/**
	 * Returns <code>true</code> iff the action can operate on the specified selection.
	 * @return <code>true</code> if the action can operate on the specified selection, 
	 *  <code>false</code> otherwise.
	 */
	public abstract boolean canOperateOn(IStructuredSelection selection);
	
	/**
	 * Returns the current selection.
	 * 
	 * @return the current selection as a structured selection or <code>null</code>
	 */
	protected IStructuredSelection getStructuredSelection() {
		return fProvider.getSelection();
	}
	
	/**
	 * Update the action's enable state according to the current selection of
	 * the used selection provider.
	 */
	public void update() {
		IStructuredSelection selection= getStructuredSelection();
		boolean enabled= false;
		if (selection != null)
			enabled= canOperateOn(selection);
			
		setEnabled(enabled);
	}	
	
	public static void activateRefactoringWizard(Refactoring refactoring, RefactoringWizard wizard, String dialogTitle) throws JavaModelException{
		RefactoringStatus status= refactoring.checkActivation(new NullProgressMonitor());
		if (! status.hasFatalError()){
			wizard.setActivationStatus(status);
			new RefactoringWizardDialog(JavaPlugin.getActiveWorkbenchShell(), wizard).open();
		} else{
			if (status.getEntries().size() == 1){
				MessageDialog.openInformation(JavaPlugin.getActiveWorkbenchShell(), dialogTitle, status.getFirstMessage(RefactoringStatus.FATAL));
			}else{
				ListDialog dialog= new ListDialog(JavaPlugin.getActiveWorkbenchShell(),	status, dialogTitle,"",
																			new RefactoringStatusContentProvider(), new RefactoringStatusEntryLabelProvider());
				dialog.open();															
			}	
		}	
	}
}

