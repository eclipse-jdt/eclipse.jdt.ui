/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */
package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jface.wizard.IWizardPage;

import org.eclipse.jdt.core.refactoring.RefactoringStatus;

/**
 * An abstract wizard page that can be used to implement user input pages for 
 * refactoring wizards. Usually user input pages are pages shown at the beginning 
 * of a wizard. As soon as the "last" user input page is left a corresponding 
 * precondition check is executed.
 */
public abstract class UserInputWizardPage extends RefactoringWizardPage {

	private boolean fIsLastUserPage;

	/**
	 * Creates a new user input page.
	 * @param name the page's name.
	 * @param isLastUserPage <code>true</code> if this page is the wizard's last
	 *  user input page. Otherwise <code>false</code>.
	 */
	public UserInputWizardPage(String name, boolean isLastUserPage) {
		super(name);
		fIsLastUserPage= isLastUserPage;
	}
	
	/* (non-JavaDoc)
	 * Method declared in IWizardPage.
	 */
	public IWizardPage getNextPage() {
		if (fIsLastUserPage) {
			return getRefactoringWizard().computeUserInputSuccessorPage();
		} else {
			return super.getNextPage();
		}
	}
	
	/* (non-JavaDoc)
	 * Method declared in IWizardPage.
	 */
	public boolean canFlipToNextPage() {
		if (fIsLastUserPage) {
			// we can't call getNextPage to determine if flipping is allowed since computing
			// the next page is quite expensive (checking preconditions and creating a
			// change). So we say yes if the page is complete.
			return isPageComplete();
		} else {
			return super.canFlipToNextPage();
		}
	}
	
	/* (non-JavaDoc)
	 * Method defined in RefactoringWizardPage
	 */
	protected boolean performFinish() {
		RefactoringWizard wizard= getRefactoringWizard();
		int threshold= RefactoringPreferences.getCheckPassedSeverity();
		
		CreateChangeOperation create= new CreateChangeOperation(getRefactoring(), CreateChangeOperation.CHECK_INPUT); 
		create.setCheckPassedSeverity(threshold);
		
		PerformChangeOperation perform= new PerformChangeOperation(create);
		perform.setCheckPassedSeverity(threshold);
		
		boolean result= wizard.performFinish(perform);
		if (!result)
			return false;
		RefactoringStatus status= create.getStatus();
		if (status.getSeverity() > threshold) {
			wizard.setStatus(status);
			wizard.setChange(perform.getChange());
			IWizardPage nextPage= wizard.getPage(ErrorWizardPage.PAGE_NAME);
			wizard.getContainer().showPage(nextPage);
			return false;
		}
		return true;	
	}		
}