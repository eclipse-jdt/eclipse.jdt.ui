/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.sef;

import org.eclipse.jdt.internal.corext.refactoring.sef.SelfEncapsulateFieldRefactoring;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.refactoring.PreviewWizardPage;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;

public class SelfEncapsulateFieldWizard extends RefactoringWizard {
	
	public SelfEncapsulateFieldWizard(SelfEncapsulateFieldRefactoring refactoring) {
		super(refactoring, RefactoringMessages.getString("SelfEncapsulateField.sef"), IJavaHelpContextIds.SEF_ERROR_WIZARD_PAGE); //$NON-NLS-1$
	}

	protected void addUserInputPages() {
		addPage(new SelfEncapsulateFieldInputPage());
	}

	protected void addPreviewPage() {
		PreviewWizardPage page= new PreviewWizardPage();
		addPage(page);
	}
}