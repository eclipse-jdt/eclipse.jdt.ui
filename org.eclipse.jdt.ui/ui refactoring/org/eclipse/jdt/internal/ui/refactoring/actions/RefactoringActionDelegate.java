/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.ui.JavaPlugin;


public class RefactoringActionDelegate implements IWorkbenchWindowActionDelegate {

	private IWorkbenchWindow fWorkbenchWindow;
	private IAction fAction;
	private RefactoringAction[] fPossibleTargets;
	private RefactoringAction fTargetAction;
	
	private String fOperationNotAvailableDialogMessage;
	private String fOperationNotAvailableDialogTitle;
	
	protected RefactoringActionDelegate() {
		this("Refactoring", "Operation not available on current selection.");
	}
	
	protected RefactoringActionDelegate(String operationNotAvailableDialogTitle, String operationNotAvailableDialogMessage) {
		Assert.isNotNull(operationNotAvailableDialogTitle);
		Assert.isNotNull(operationNotAvailableDialogMessage);
		fOperationNotAvailableDialogTitle= operationNotAvailableDialogTitle;
		fOperationNotAvailableDialogMessage= operationNotAvailableDialogMessage;
	}
	
	protected void initPossibleTargets(RefactoringAction[] possibleTargets) {
		fPossibleTargets= possibleTargets;
	}
	
	/* (non-Javadoc)
	 * Method declared in IActionDelegate
	 */
	public void run(IAction action) {
		fTargetAction= findAction();
		if (fTargetAction != null) {
			fTargetAction.run();
		} else {
			MessageDialog.openInformation(JavaPlugin.getActiveWorkbenchShell(), 
				fOperationNotAvailableDialogTitle,
				fOperationNotAvailableDialogMessage);
		}
	}
		
	/* (non-Javadoc)
	 * Method declared in IActionDelegate
	 */
	public void selectionChanged(IAction action, ISelection s) {
	}
	
	/* (non-Javadoc)
	 * Method declared in IActionDelegate
	 */
	public void dispose() {
	}
	
	/* (non-Javadoc)
	 * Method declared in IActionDelegate
	 */
	public void init(IWorkbenchWindow window) {
		fWorkbenchWindow= window;
	}
	
	private RefactoringAction findAction() {
		if (fPossibleTargets == null)
			return null;
		for (int i= 0; i < fPossibleTargets.length; i++) {
			RefactoringAction action= fPossibleTargets[i];
			action.update();
			if (action.isEnabled())
				return action;
		}
		return null;
	}
}

