package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

import org.eclipse.jdt.internal.corext.refactoring.structure.PullUpRefactoring;

public class PullUpWizard extends RefactoringWizard {

	public PullUpWizard(PullUpRefactoring ref, String pageTitle, String errorPageContextHelpId) {
		super(ref, pageTitle, errorPageContextHelpId);
		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_REFACTOR_PULL_UP);
	}
	
	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		addPage(new PullUpInputPage1());
		addPage(new PullUpInputPage2());
	}

	private PullUpRefactoring getPullUpRefactoring(){
		return (PullUpRefactoring)getRefactoring();
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard#hasMultiPageUserInput()
	 */
	public boolean hasMultiPageUserInput() {
		return true;
	}

}