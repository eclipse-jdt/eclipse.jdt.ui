/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.refactoring;import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IMultiRenameRefactoring;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;

public class RenameParametersWizard extends RefactoringWizard {
	
	private String fMessage;
	
	public RenameParametersWizard(IMultiRenameRefactoring refactoring, String helpId, String title, String message){
		super((Refactoring)refactoring, title, helpId);
		fMessage= message;
	}

	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		RenameParametersWizardPage page= new RenameParametersWizardPage(true);
		page.setMessage(fMessage);
		addPage(page);
	}
	
	/* non java-doc
	 * @see RefactoringWizard#addPreviewPage
	 */ 
	protected void addPreviewPage(){
		NewPreviewWizardPage page= new NewPreviewWizardPage();
		page.setExpandFirstNode(true);
		addPage(page);
	}
}