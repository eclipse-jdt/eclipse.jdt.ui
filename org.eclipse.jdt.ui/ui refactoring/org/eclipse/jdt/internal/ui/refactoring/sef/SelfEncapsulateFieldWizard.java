/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.sef;

import org.eclipse.jdt.internal.corext.refactoring.sef.SelfEncapsulateFieldRefactoring;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.refactoring.NewPreviewWizardPage;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
public class SelfEncapsulateFieldWizard extends RefactoringWizard {
	
	public SelfEncapsulateFieldWizard(SelfEncapsulateFieldRefactoring refactoring) {
		super(refactoring, "Self Encapsulate Field", IJavaHelpContextIds.SEF_WIZARD_PAGE);
	}

	protected void addUserInputPages() {
		addPage(new SelfEncapsulateFieldInputPage());
	}

	protected void addPreviewPage() {
		NewPreviewWizardPage page= new NewPreviewWizardPage();
		addPage(page);
	}
}