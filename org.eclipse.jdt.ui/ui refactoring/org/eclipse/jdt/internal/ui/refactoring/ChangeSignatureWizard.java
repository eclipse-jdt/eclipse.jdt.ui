package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jdt.internal.corext.refactoring.structure.ChangeSignatureRefactoring;

public class ChangeSignatureWizard extends RefactoringWizard {

	public ChangeSignatureWizard(ChangeSignatureRefactoring ref, String pageTitle, String errorPageContextHelpId) {
		super(ref, pageTitle, errorPageContextHelpId);
	}

	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		addPage(new ChangeSignatureInputPage());
	}
}
