/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */
package org.eclipse.jdt.internal.ui.refactoring.undo;

import java.lang.reflect.InvocationTargetException;import org.eclipse.jface.operation.IRunnableWithProgress;import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.core.refactoring.ChangeContext;import org.eclipse.jdt.core.refactoring.Refactoring;

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

	public IRunnableWithProgress createOperation(final ChangeContext context) {
		return new IRunnableWithProgress(){
			public void run(IProgressMonitor pm) throws InvocationTargetException {
				try {
					Refactoring.getUndoManager().performUndo(context, pm);
				} catch (JavaModelException e) {
					throw new InvocationTargetException(e);			
				}
			}

		};
	}
}
