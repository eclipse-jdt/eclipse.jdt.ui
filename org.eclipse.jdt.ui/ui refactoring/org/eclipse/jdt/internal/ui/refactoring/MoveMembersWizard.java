package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jdt.internal.corext.refactoring.structure.MoveMembersRefactoring;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;

public class MoveMembersWizard extends RefactoringWizard {

	public MoveMembersWizard(MoveMembersRefactoring ref, String pageTitle, String errorPageContextHelpId) {
		super(ref, pageTitle, errorPageContextHelpId);
	}

	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		setPageTitle(RefactoringMessages.getString("MoveMembersWizard.page_title")); //$NON-NLS-1$
		addPage(new MoveMembersInputPage());
	}
	
	private MoveMembersRefactoring getMoveMembersRefactoring(){
		return (MoveMembersRefactoring)getRefactoring();
	}
}
