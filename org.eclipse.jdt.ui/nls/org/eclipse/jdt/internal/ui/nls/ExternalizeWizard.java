/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.nls;

import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.ui.nls.model.NLSRefactoring;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;

class ExternalizeWizard extends RefactoringWizard {

	public ExternalizeWizard(Refactoring refactoring) {
		//XXX needs help context
		super(refactoring, Messages.getString("wizard.Externalize_strings_1"), "HELP_CONTEXT"); //$NON-NLS-2$ //$NON-NLS-1$
		setWindowTitle(Messages.getString("wizard.Externalize_strings_1"));//$NON-NLS-1$
	}
	
	/*
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		NLSRefactoring ref= (NLSRefactoring)getRefactoring();
		setPageTitle(getPageTitle() + Messages.getString("wizard._in")+ "\"" + ref.getCu().getElementName() + "\""); //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
		ExternalizeWizardPage page= new ExternalizeWizardPage();
		page.setMessage(Messages.getString("wizard.Select_strings_to_externalize_3")); //$NON-NLS-1$
		addPage(page);
		
		ExternalizeWizardPage2 page2= new ExternalizeWizardPage2();
		page2.setMessage(Messages.getString("wizard.Select_values_4")); //$NON-NLS-1$
		addPage(page2);
	} 

	/*
	 * @see RefactoringWizard#checkActivationOnOpen
	 */ 
	protected boolean checkActivationOnOpen() {
		return true;
	}
}

