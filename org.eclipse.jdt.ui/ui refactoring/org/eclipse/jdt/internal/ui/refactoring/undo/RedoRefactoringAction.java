/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.refactoring.undo;

import java.lang.reflect.InvocationTargetException;import org.eclipse.jface.operation.IRunnableWithProgress;import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.internal.core.refactoring.base.ChangeAbortException;import org.eclipse.jdt.internal.core.refactoring.base.ChangeContext;import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;

public class RedoRefactoringAction extends UndoManagerAction {

	private String fText;

	public RedoRefactoringAction() {
		super(RefactoringMessages.getString("RedoRefactoringAction.label")); //$NON-NLS-1$
		fText= getText();
		setDescription(RefactoringMessages.getString("RedoRefactoringAction.description")); //$NON-NLS-1$
		setToolTipText(RefactoringMessages.getString("RedoRefactoringAction.tooltip")); //$NON-NLS-1$
	}

	public boolean isEnabled(){
		return Refactoring.getUndoManager().anythingToRedo();
	}

	public void update() {
		String text= Refactoring.getUndoManager().peekRedoName();
		if (text != null) {
			text= fText + " - " + text; //$NON-NLS-1$
		} else {
			text= fText;
		}
		setText(text);
	}

	protected String getName() {
		// PR: 1GEWDUH: ITPJCORE:WINNT - Refactoring - Unable to undo refactor change
		return RefactoringMessages.getString("RedoRefactoringAction.name"); //$NON-NLS-1$
	}
	
	public IRunnableWithProgress createOperation(final ChangeContext context) {
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
}
