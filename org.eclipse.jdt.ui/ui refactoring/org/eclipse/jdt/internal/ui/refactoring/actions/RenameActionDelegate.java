/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.reorg.RenameAction;
import org.eclipse.jdt.internal.ui.reorg.ReorgAction;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

public class RenameActionDelegate implements IWorkbenchWindowActionDelegate {

	private IWorkbenchWindow fWorkbenchWindow;
	private IAction fAction;
	private RefactoringAction[] fRenameActions;
	private RefactoringAction fTargetAction;

	public RenameActionDelegate() {
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
				"Rename Refactoring",
				"Operation not available on current selection.");
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
		ISelectionService service= fWorkbenchWindow.getSelectionService();
		fRenameActions= new RefactoringAction[1];
		fRenameActions[0]= new RenameAction(StructuredSelectionProvider.createFrom(service));
	}
	
	private RefactoringAction findAction() {
		for (int i= 0; i < fRenameActions.length; i++) {
			RefactoringAction action= fRenameActions[i];
			action.update();
			if (action.isEnabled())
				return action;
		}
		return null;
	}
}

