package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jdt.internal.ui.reorg.ReorgMessages;

import org.eclipse.jdt.internal.corext.refactoring.structure.MoveStaticMembersRefactoring;

public class MoveMembersWizard extends RefactoringWizard {

	public MoveMembersWizard(MoveStaticMembersRefactoring ref) {
		super(
			ref, 
			ReorgMessages.getString("moveAction.name"), //$NON-NLS-1$
			ReorgMessages.getString("moveAction.destination.label") //$NON-NLS-1$
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
	
	private MoveStaticMembersRefactoring getMoveMembersRefactoring(){
		return (MoveStaticMembersRefactoring)getRefactoring();
	}
}
