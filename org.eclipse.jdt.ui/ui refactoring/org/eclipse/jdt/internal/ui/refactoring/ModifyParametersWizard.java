package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jdt.internal.corext.refactoring.structure.ModifyParametersrRefactoring;

public class ModifyParametersWizard extends RefactoringWizard {

	public ModifyParametersWizard(ModifyParametersrRefactoring ref, String pageTitle, String errorPageContextHelpId) {
		super(ref, pageTitle, errorPageContextHelpId);
	}

	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		addPage(new ModifyParametersInputPage());
	}
}
