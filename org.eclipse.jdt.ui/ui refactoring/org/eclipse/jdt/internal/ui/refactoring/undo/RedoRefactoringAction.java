/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */
package org.eclipse.jdt.internal.ui.refactoring.undo;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.refactoring.Refactoring;

public class RedoRefactoringAction extends UndoManagerAction {

	private static final String PREFIX= "Refactoring.RedoRefactoring.";
	private String fText;

	public RedoRefactoringAction() {
		super(PREFIX);
		fText= getText();
	}

	public boolean canActionBeAdded() {
		return Refactoring.getUndoManager().anythingToRedo();
	}

	public void update() {
		String text= Refactoring.getUndoManager().peekRedoName();
		if (text != null) {
			text= fText + " " + text;
		} else {
			text= fText;
		}
		setText(text);
	}

	public IRunnableWithProgress createOperation() {
		return new IRunnableWithProgress(){
			public void run(IProgressMonitor pm) throws InvocationTargetException {
				try {
					Refactoring.getUndoManager().performRedo(pm);
				} catch (JavaModelException e) {
					throw new InvocationTargetException(e);			
				}
			}

		};
	}
}
