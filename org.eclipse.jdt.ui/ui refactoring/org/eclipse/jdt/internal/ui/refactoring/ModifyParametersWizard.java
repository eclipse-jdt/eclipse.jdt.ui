package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jdt.internal.corext.refactoring.structure.ModifyParametersRefactoring;

public class ModifyParametersWizard extends RefactoringWizard {

	public ModifyParametersWizard(ModifyParametersRefactoring ref, String pageTitle, String errorPageContextHelpId) {
		super(ref, pageTitle, errorPageContextHelpId);
	}

	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		addPage(new ModifyParametersInputPage());
	}
}
