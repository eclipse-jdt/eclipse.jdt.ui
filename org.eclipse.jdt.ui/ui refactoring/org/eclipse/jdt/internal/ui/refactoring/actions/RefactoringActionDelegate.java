/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import org.eclipse.jdt.internal.corext.refactoring.Assert;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;


public class RefactoringActionDelegate implements IWorkbenchWindowActionDelegate {

	private IWorkbenchWindow fWorkbenchWindow;
	private IRefactoringAction[] fPossibleTargets;
	
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
	
	protected void initPossibleTargets(IRefactoringAction[] possibleTargets) {
		fPossibleTargets= possibleTargets;
	}
	
	protected boolean handleTextSelection(ITextSelection selection) {
		return (fWorkbenchWindow.getPartService().getActivePart() instanceof CompilationUnitEditor);
	}
	
	protected boolean handleStructuredSelection(IStructuredSelection selection) {
		// XXX Workaround for http://bugs.eclipse.org/bugs/show_bug.cgi?id=7823
		if (!((IStructuredSelection)selection).isEmpty())
			return findAction() != null;
		return false;
	}
	
	/* (non-Javadoc)
	 * Method declared in IActionDelegate
	 */
	public void run(IAction action) {
		IRefactoringAction targetAction= findAction();
		if (targetAction != null) {
			targetAction.run();
		} else {
			MessageDialog.openInformation(fWorkbenchWindow.getShell(), 
				fOperationNotAvailableDialogTitle,
				fOperationNotAvailableDialogMessage);
		}
	}
		
	/* (non-Javadoc)
	 * Method declared in IActionDelegate
	 */
	public void selectionChanged(IAction action, ISelection s) {
		boolean enabled= false;
		if (s instanceof ITextSelection) {
			enabled= handleTextSelection((ITextSelection)s);
		} else if (s instanceof IStructuredSelection) {
			enabled= handleStructuredSelection((IStructuredSelection)s);
		}
		action.setEnabled(enabled);
	}
	
	/* (non-Javadoc)
	 * Method declared in IActionDelegate
	 */
	public void dispose() {
		fWorkbenchWindow= null;
	}
	
	/* (non-Javadoc)
	 * Method declared in IActionDelegate
	 */
	public void init(IWorkbenchWindow window) {
		fWorkbenchWindow= window;
	}
	
	protected IRefactoringAction findAction() {
		if (fPossibleTargets == null)
			return null;
		for (int i= 0; i < fPossibleTargets.length; i++) {
			IRefactoringAction action= fPossibleTargets[i];
			action.update();
			if (action.isEnabled())
				return action;
		}
		return null;
	}
}

