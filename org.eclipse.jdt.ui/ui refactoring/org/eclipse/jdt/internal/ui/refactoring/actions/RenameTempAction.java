/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameTempRefactoring;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
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
	
	/* (non-Javadoc)
	 * @see TextSelectionAction#canOperateOnCurrentSelection(ISelection)
	 */
	protected boolean canOperateOnCurrentSelection(ISelection selection) {
		if (!(selection instanceof ITextSelection))
			return false;
		
		//must check it here - see bug 12590
		//if it's not a local variable - we will try to resolve the symbol as a IJavaElement in rename action
		Refactoring renameTempRefactoring= createRefactoring(getCompilationUnit(), (ITextSelection)selection);
		try {
			return (renameTempRefactoring.checkActivation(new NullProgressMonitor()).isOK());
		} catch(JavaModelException e) {
			//ignore the exception
			return false;
		}
	}
	
}

