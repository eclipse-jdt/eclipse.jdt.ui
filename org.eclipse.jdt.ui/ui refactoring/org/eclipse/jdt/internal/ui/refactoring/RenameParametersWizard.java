/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.refactoring;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;import org.eclipse.jdt.internal.ui.refactoring.RenameInputWizardPage;import org.eclipse.jdt.internal.ui.refactoring.TextInputWizardPage;import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;import org.eclipse.jdt.internal.core.refactoring.methods.RenameParametersRefactoring;

public class RenameParametersWizard extends RefactoringWizard {
	
	public RenameParametersWizard(){
		super(getTitle(), IJavaHelpContextIds.RENAME_PARAMS_ERROR_WIZARD_PAGE);
	}

	/**
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		RenameParametersWizardPage page= new RenameParametersWizardPage(true);
		page.setMessage(getMessage());
		addPage(page);
	}
	
	/**
	 * @see RefactoringWizard#addPreviewPage
	 */ 
	protected void addPreviewPage(){
		PreviewWizardPage page= new PreviewWizardPage();
		page.setExpandFirstNode(true);
		addPage(page);
	}
	
	private static String getTitle(){
		return RefactoringMessages.getString("RenameParametersWizard.title"); //$NON-NLS-1$
	}

	private static String getMessage(){
		return RefactoringMessages.getString("RenameParametersWizard.message"); //$NON-NLS-1$
	}
	
	protected boolean checkActivationOnOpen() {
		return true;
	}
}