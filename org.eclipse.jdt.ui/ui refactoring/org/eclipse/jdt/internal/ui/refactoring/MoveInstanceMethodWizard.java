package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;

import org.eclipse.jdt.internal.corext.refactoring.structure.MoveInstanceMethodRefactoring;

public class MoveInstanceMethodWizard extends RefactoringWizard {

	public MoveInstanceMethodWizard(MoveInstanceMethodRefactoring ref) {
		super(ref, "Move Method", IJavaHelpContextIds.MOVE_MEMBERS_ERROR_WIZARD_PAGE);
	}

	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		addPage(new MoveInstanceMethodInputPage());
	}
}
