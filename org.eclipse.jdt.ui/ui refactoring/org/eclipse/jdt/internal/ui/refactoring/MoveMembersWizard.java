package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jdt.internal.corext.refactoring.structure.MoveStaticMembersRefactoring;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;

public class MoveMembersWizard extends RefactoringWizard {

	public MoveMembersWizard(MoveStaticMembersRefactoring ref) {
		this(
			ref, 
			RefactoringMessages.getString("RefactoringGroup.move_Members"), //$NON-NLS-1$
			IJavaHelpContextIds.MOVE_MEMBERS_ERROR_WIZARD_PAGE
		);
	}

	public MoveMembersWizard(MoveStaticMembersRefactoring ref, String pageTitle, String errorPageContextHelpId) {
		super(ref, pageTitle, errorPageContextHelpId);
	}

	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		setPageTitle(RefactoringMessages.getString("MoveMembersWizard.page_title")); //$NON-NLS-1$
		addPage(new MoveMembersInputPage());
	}
}
