package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jdt.internal.core.refactoring.tagging.IRenameRefactoring;

public class RenameRefactoringWizard2 extends RenameRefactoringWizard {

	/**
	 * Constructor for RenameRefactoringWizard2.
	 * @param ref
	 * @param title
	 * @param message
	 * @param pageContextHelpId
	 * @param errorContextHelpId
	 */
	public RenameRefactoringWizard2(IRenameRefactoring ref, String title, 	String message, 
																  String pageContextHelpId, String errorContextHelpId) {
		super(ref, title, message, pageContextHelpId, errorContextHelpId);
	}

	protected void addPreviewPage() {
		NewPreviewWizardPage page= new NewPreviewWizardPage();
		addPage(page);
	}
}

