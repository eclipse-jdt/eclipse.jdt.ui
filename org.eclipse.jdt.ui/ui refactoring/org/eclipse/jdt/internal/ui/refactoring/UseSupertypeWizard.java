package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jdt.internal.corext.refactoring.structure.UseSupertypeWherePossibleRefactoring;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;

public class UseSupertypeWizard extends RefactoringWizard{

	public UseSupertypeWizard(UseSupertypeWherePossibleRefactoring ref) {
		super(ref, RefactoringMessages.getString("UseSupertypeWizard.Use_Super_Type_Where_Possible"), IJavaHelpContextIds.USE_SUPERTYPE_ERROR_WIZARD_PAGE); //$NON-NLS-1$
	}

	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		addPage(new UseSupertypeInputPage());
	}
}
