package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.rename.InlineTempRefactoring;

public class InlineTempWizard extends RefactoringWizard {

	/**
	 * Constructor for InlineTempWizard.
	 * @param ref
	 * @param pageTitle
	 * @param errorPageContextHelpId
	 */
	public InlineTempWizard(InlineTempRefactoring ref, String pageTitle, String errorPageContextHelpId) {
		super(ref, pageTitle, errorPageContextHelpId);
	}

	/* non java-doc
	 * @see RefactoringWizard#addPreviewPage
	 */ 
	protected void addPreviewPage() {
		addPage(new NewPreviewWizardPage());
	}

}

