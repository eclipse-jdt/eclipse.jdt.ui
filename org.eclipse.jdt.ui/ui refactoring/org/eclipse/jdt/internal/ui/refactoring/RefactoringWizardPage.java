/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jface.util.Assert;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardPage;

import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;

public abstract class RefactoringWizardPage extends WizardPage {

	protected RefactoringWizardPage(String name) {
		super(name);
	}
	
	/* (non-Javadoc)
	 * Method declared on IWizardPage.
	 */
	public void setWizard(IWizard newWizard) {
		Assert.isTrue(newWizard instanceof RefactoringWizard);
		super.setWizard(newWizard);
	}

	/**
	 * Returns the refactoring used by the wizard to which this page belongs.
	 * Returns <code>null</code> if the page isn't added to any wizard yet.
	 */
	protected Refactoring getRefactoring() {
		IWizard wizard= getWizard();
		if (wizard == null)
			return null;
		return ((RefactoringWizard)wizard).getRefactoring();
	}
	
	/**
	 * Returns the page's refactoring wizard.
	 */
	protected RefactoringWizard getRefactoringWizard() {
		return (RefactoringWizard)getWizard();
	}
	
	/**
	 * The user has pressed the finish button. Perform the page specific finish
	 * action. Returns <code>true</code> if finish operation ended without errors,
	 * otherwise <code>false</code> is returned.
	 */
	protected boolean performFinish() {
		return true;
	} 
}