/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public abstract class TextSelectionBasedRefactoringAction extends TextSelectionAction implements IRefactoringAction{

	protected TextSelectionBasedRefactoringAction(String name) {
		super(name);
	}
	
	/* (non-JavaDoc)
	 * Method declared in IAction.
	 */
	public void run() {
		try{
			Refactoring refactoring= createRefactoring(getCompilationUnit(), getTextSelection());
			new RefactoringStarter().activate(refactoring, createWizard(refactoring), getMessageDialogTitle(), false);
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, getText(), RefactoringMessages.getString("TextSelectionBasedRefactoringAction.exception")); //$NON-NLS-1$
		}	
	}
	
	protected abstract Refactoring createRefactoring(ICompilationUnit cunit, ITextSelection selection);
	
	protected abstract RefactoringWizard createWizard(Refactoring refactoring);
	
	protected abstract String getMessageDialogTitle();
}

