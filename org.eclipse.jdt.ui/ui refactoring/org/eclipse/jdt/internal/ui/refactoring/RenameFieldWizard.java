package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IRenameRefactoring;

class RenameFieldWizard extends RenameRefactoringWizard {

	public RenameFieldWizard(IRenameRefactoring ref, String title, String message, String pageContextHelpId, String errorContextHelpId) {
		super(ref, title, message, pageContextHelpId, errorContextHelpId);
	}

	/* non java-doc
	 * @see RenameRefactoringWizard#createInputPage
	 */ 
	protected RenameInputWizardPage createInputPage(String message, String initialSetting) {
		return new RenameFieldInputWizardPage(message, getPageContextHelpId(), initialSetting) {
			protected RefactoringStatus validateTextField(String text) {
				RefactoringStatus result= validateNewName(text);
				updateGetterSetterLabels();
				return result;
			}	
		};
	}
}
