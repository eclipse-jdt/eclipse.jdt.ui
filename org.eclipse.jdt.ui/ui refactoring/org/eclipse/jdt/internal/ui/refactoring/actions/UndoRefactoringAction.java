/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.refactoring.actions;

import java.lang.reflect.InvocationTargetException;import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.internal.corext.refactoring.base.ChangeAbortException;import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;import org.eclipse.jdt.internal.corext.refactoring.base.IUndoManagerListener;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.UndoManagerAdapter;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;

public class UndoRefactoringAction extends UndoManagerAction {

	private IAction fAction;

	public UndoRefactoringAction() {
	}

	protected String getName() {
		// PR: 1GEWDUH: ITPJCORE:WINNT - Refactoring - Unable to undo refactor change
		return RefactoringMessages.getString("UndoRefactoringAction.name"); //$NON-NLS-1$
	}
	
	public IRunnableWithProgress createOperation(final ChangeContext context) {
		// PR: 1GEWDUH: ITPJCORE:WINNT - Refactoring - Unable to undo refactor change
		return new IRunnableWithProgress(){
			public void run(IProgressMonitor pm) throws InvocationTargetException {
				try {
					setPreflightStatus(Refactoring.getUndoManager().performUndo(context, pm));
				} catch (JavaModelException e) {
					throw new InvocationTargetException(e);			
				} catch (ChangeAbortException e) {
					throw new InvocationTargetException(e);
				}
			}

		};
	}
	
	/* (non-Javadoc)
	 * Method declared in IActionDelegate
	 */
	public void run(IAction action) {
		hookListener(action);
		if (!Refactoring.getUndoManager().anythingToUndo()) {
			MessageDialog.openInformation(JavaPlugin.getActiveWorkbenchShell(), 
				"Undo Refactoring",
				"Nothing to undo");
			fAction.setEnabled(false);	
			return;
		}
		internalRun();
	}
	
	/* (non-Javadoc)
	 * Method declared in IActionDelegate
	 */
	public void dispose() {
		fAction= null;
	}
	
	private void hookListener(IAction action) {
		if (fAction != null)
			return;
		fAction= action;
		Refactoring.getUndoManager().addListener(new UndoManagerAdapter() {
			public void undoAdded() {
				if (fAction == null)
					return;
				fAction.setEnabled(true);
				fAction.setText(RefactoringMessages.getFormattedString(
					"UndoRefactoringAction.extendedLabel",
					Refactoring.getUndoManager().peekUndoName()));
			}
			public void noMoreUndos() {
				if (fAction == null)
					return;
				fAction.setText(RefactoringMessages.getString("UndoRefactoringAction.label"));
				fAction.setEnabled(false);
			}
			
		});
	}	
}
