package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jdt.internal.corext.refactoring.structure.UseSupertypeWherePossibleRefactoring;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;

public class UseSupertypeWizard extends RefactoringWizard{

	public UseSupertypeWizard(UseSupertypeWherePossibleRefactoring ref) {
		super(ref, "Use Super Type Where Possible", IJavaHelpContextIds.USE_SUPERTYPE_ERROR_WIZARD_PAGE);
	}

	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		addPage(new UseSupertypeInputPage());
	}
}
