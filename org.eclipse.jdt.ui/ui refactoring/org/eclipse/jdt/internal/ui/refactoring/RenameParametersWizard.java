/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.refactoring;import org.eclipse.jdt.internal.core.refactoring.rename.RenameParametersRefactoring;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;

public class RenameParametersWizard extends RefactoringWizard {
	
	public RenameParametersWizard(RenameParametersRefactoring refactoring){
		super(refactoring, getTitle(), IJavaHelpContextIds.RENAME_PARAMS_ERROR_WIZARD_PAGE);
	}

	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		RenameParametersWizardPage page= new RenameParametersWizardPage(true);
		page.setMessage(getMessage());
		addPage(page);
	}
	
	/* non java-doc
	 * @see RefactoringWizard#addPreviewPage
	 */ 
	protected void addPreviewPage(){
		PreviewWizardPage page= new PreviewWizardPage();
		page.setExpandFirstNode(true);
		addPage(page);
	}
	
	private static String getTitle(){
		return RefactoringMessages.getString("RenameParametersWizard.title"); //$NON-NLS-1$
	}

	private static String getMessage(){
		return RefactoringMessages.getString("RenameParametersWizard.message"); //$NON-NLS-1$
	}
	
}