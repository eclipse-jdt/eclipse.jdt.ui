package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jdt.internal.corext.refactoring.structure.PullUpMethodRefactoring;

public class PullUpMethodsWizard extends RefactoringWizard {

	public PullUpMethodsWizard(PullUpMethodRefactoring ref, String pageTitle, String errorPageContextHelpId) {
		super(ref, pageTitle, errorPageContextHelpId);
	}
	
	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		addPage(new PullUpMethodsInputPage());
	}
}

