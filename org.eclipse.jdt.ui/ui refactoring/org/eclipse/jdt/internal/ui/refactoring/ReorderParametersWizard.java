package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jdt.internal.corext.refactoring.structure.ReorderParametersRefactoring;

public class ReorderParametersWizard extends RefactoringWizard {

	public ReorderParametersWizard(ReorderParametersRefactoring ref, String pageTitle, String errorPageContextHelpId) {
		super(ref, pageTitle, errorPageContextHelpId);
	}

	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		addPage(new ReorderParametersInputPage());
	}
}
