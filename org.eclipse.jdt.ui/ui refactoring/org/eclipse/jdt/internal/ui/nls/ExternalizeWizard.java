/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.nls;

import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.core.nls.model.NLSRefactoring;
import org.eclipse.jdt.internal.ui.refactoring.NewPreviewWizardPage;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;

class ExternalizeWizard extends RefactoringWizard {

	public ExternalizeWizard(Refactoring refactoring) {
		//XXX needs help context
		super(refactoring, Messages.getString("wizard.name"), "HELP_CONTEXT"); //$NON-NLS-2$ //$NON-NLS-1$
		setWindowTitle(Messages.getString("wizard.name"));//$NON-NLS-1$
	}
	
	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		NLSRefactoring ref= (NLSRefactoring)getRefactoring();
		setPageTitle(getPageTitle() + Messages.getString("wizard.in")+ "\"" + ref.getCu().getElementName() + "\""); //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
		ExternalizeWizardPage page= new ExternalizeWizardPage();
		page.setMessage(Messages.getString("wizard.select")); //$NON-NLS-1$
		addPage(page);
		
		ExternalizeWizardPage2 page2= new ExternalizeWizardPage2();
		page2.setMessage(Messages.getString("wizard.select_values")); //$NON-NLS-1$
		addPage(page2);
	} 


	/* non java-doc
	 * @see RefactoringWizard#addPreviewPage
	 */ 
	protected void addPreviewPage(){
		addPage(new NewPreviewWizardPage());
	}

	/* non java-doc
	 * @see RefactoringWizard#checkActivationOnOpen
	 */ 
	protected boolean checkActivationOnOpen() {
		return true;
	}
}

