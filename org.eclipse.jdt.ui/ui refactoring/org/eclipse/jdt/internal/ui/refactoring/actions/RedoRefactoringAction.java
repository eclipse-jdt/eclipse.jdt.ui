/* 
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.refactoring.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.base.ChangeAbortException;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.UndoManagerAdapter;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;

public class RedoRefactoringAction extends UndoManagerAction {

	public RedoRefactoringAction() {
	}

	/* (non-Javadoc)
	 * Method declared in UndoManagerAction
	 */
	protected String getName() {
		// PR: 1GEWDUH: ITPJCORE:WINNT - Refactoring - Unable to undo refactor change
		return RefactoringMessages.getString("RedoRefactoringAction.name"); //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * Method declared in UndoManagerAction
	 */
	protected IRunnableWithProgress createOperation(final ChangeContext context) {
		// PR: 1GEWDUH: ITPJCORE:WINNT - Refactoring - Unable to undo refactor change
		return new IRunnableWithProgress(){
			public void run(IProgressMonitor pm) throws InvocationTargetException {
				try {
					setPreflightStatus(Refactoring.getUndoManager().performRedo(context, pm));
				} catch (JavaModelException e) {
					throw new InvocationTargetException(e);			
				} catch (ChangeAbortException e) {
					throw new InvocationTargetException(e);
				}
			}

		};
	}
	
	/* (non-Javadoc)
	 * Method declared in UndoManagerAction
	 */
	protected UndoManagerAdapter createUndoManagerListener() {
		return new UndoManagerAdapter() {
			public void redoAdded() {
				IAction action= getAction();
				if (action == null)
					return;
				action.setEnabled(true);
				action.setText(RefactoringMessages.getFormattedString(
					"RedoRefactoringAction.extendedLabel",
					Refactoring.getUndoManager().peekRedoName()));
			}
			public void noMoreRedos() {
				IAction action= getAction();
				if (action == null)
					return;
				action.setText(RefactoringMessages.getString("RedoRefactoringAction.label"));
				action.setEnabled(false);
			}
			
		};
	}
		
	/* (non-Javadoc)
	 * Method declared in IActionDelegate
	 */
	public void selectionChanged(IAction action, ISelection s) {
		if (!isHooked()) {
			hookListener(action);
			action.setEnabled(Refactoring.getUndoManager().anythingToRedo());
		}
	}	
}
