/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameTempRefactoring;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.RenameRefactoringWizard;

class RenameTempAction extends TextSelectionBasedRefactoringAction{

	public RenameTempAction() {
		super("Rename Local Variable");
	}
	
	/* (non-Javadoc)
	 * Method declated in TextSelectionBasedRefactoringAction
	 */	
	protected Refactoring createRefactoring(ICompilationUnit cunit, ITextSelection selection) {
		return new RenameTempRefactoring(cunit, selection.getOffset(), selection.getLength());
	}
	
	/* (non-Javadoc)
	 * Method declated in TextSelectionBasedRefactoringAction
	 */
	protected RefactoringWizard createWizard(Refactoring refactoring) {
		String message= "Choose a new name for the local variable.";
		String wizardPageHelp= IJavaHelpContextIds.RENAME_TEMP_WIZARD_PAGE; 
		String errorPageHelp= IJavaHelpContextIds.RENAME_TEMP_ERROR_WIZARD_PAGE;
		return new RenameRefactoringWizard((RenameTempRefactoring)refactoring, getText(), message, wizardPageHelp, errorPageHelp);
	}
	
	/* (non-Javadoc)
	 * Method declated in TextSelectionBasedRefactoringAction
	 */	
	protected String getMessageDialogTitle() {
		return "Rename Local Variable";
	}
}

