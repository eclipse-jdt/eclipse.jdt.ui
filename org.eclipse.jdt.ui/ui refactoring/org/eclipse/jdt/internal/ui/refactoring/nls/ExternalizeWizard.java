/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.nls;

import org.eclipse.jface.wizard.IWizardContainer;

import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSRefactoring;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.refactoring.PreviewWizardPage;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizardDialog;

public class ExternalizeWizard extends RefactoringWizard {

	public ExternalizeWizard(Refactoring refactoring) {
		super(refactoring, NLSUIMessages.getString("wizard.name"), IJavaHelpContextIds.EXTERNALIZE_ERROR_WIZARD_PAGE); //$NON-NLS-1$
		setWindowTitle(NLSUIMessages.getString("wizard.name"));//$NON-NLS-1$
		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_EXTERNALIZE_STRINGS);
	}
	
	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		NLSRefactoring ref= (NLSRefactoring)getRefactoring();
		setPageTitle(NLSUIMessages.getFormattedString("wizard.page.title", ref.getCu().getElementName())); //$NON-NLS-1$
		ExternalizeWizardPage page= new ExternalizeWizardPage();
		page.setMessage(NLSUIMessages.getString("wizard.select")); //$NON-NLS-1$
		addPage(page);
		
		ExternalizeWizardPage2 page2= new ExternalizeWizardPage2();
		page2.setMessage(NLSUIMessages.getString("wizard.select_values")); //$NON-NLS-1$
		addPage(page2);
	} 

	/* non java-doc
	 * @see RefactoringWizard#checkActivationOnOpen
	 */ 
	protected boolean checkActivationOnOpen() {
		return true;
	}
	
	/*
	 * @see IWizard#setContainer(IWizardContainer)
	 */
	public void setContainer(IWizardContainer wizardContainer) {
		super.setContainer(wizardContainer);
		if (wizardContainer instanceof RefactoringWizardDialog){
			RefactoringWizardDialog dialog= (RefactoringWizardDialog)wizardContainer;
			dialog.setMakeNextButtonDefault(true);
		}
	}

}

