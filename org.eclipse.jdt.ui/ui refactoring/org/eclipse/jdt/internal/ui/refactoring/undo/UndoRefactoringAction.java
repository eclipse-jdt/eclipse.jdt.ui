/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.refactoring.undo;

import java.lang.reflect.InvocationTargetException;import org.eclipse.jface.operation.IRunnableWithProgress;import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.internal.core.refactoring.base.ChangeAbortException;import org.eclipse.jdt.internal.core.refactoring.base.ChangeContext;import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;

public class UndoRefactoringAction extends UndoManagerAction {

	private static final String PREFIX= "Refactoring.UndoRefactoring.";
	private String fText;

	public UndoRefactoringAction() {
		super(PREFIX);
		fText= getText();
	}

	public boolean canActionBeAdded() {
		return Refactoring.getUndoManager().anythingToUndo();
	}
	
	public void update() {
		String text= Refactoring.getUndoManager().peekUndoName();
		if (text != null) {
			text= fText + " - " + text;
		} else {
			text= fText;
		}
		setText(text);
	}

	protected String getName() {
		// PR: 1GEWDUH: ITPJCORE:WINNT - Refactoring - Unable to undo refactor change
		return "Undo";
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
}
