/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.reorg.MoveAction;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

public class MoveActionDelegate implements IWorkbenchWindowActionDelegate {

	private IWorkbenchWindow fWorkbenchWindow;
	private IAction fAction;
	private RefactoringAction fMoveAction;
	private RefactoringAction fTargetAction;

	public MoveActionDelegate() {
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
				"Move Refactoring",
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
		fMoveAction= new MoveAction(StructuredSelectionProvider.createFrom(service));
	}
	
	private RefactoringAction findAction() {
		fMoveAction.update();
		if (fMoveAction.isEnabled())
			return fMoveAction;
			
		return null;
	}
}