package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.code.ExtractTempRefactoring;

public class ExtractTempWizard extends RefactoringWizard {

	public ExtractTempWizard(ExtractTempRefactoring ref, String pageTitle, String errorPageContextHelpId) {
		super(ref, pageTitle, errorPageContextHelpId);
		setExpandFirstNode(true);
	}

	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		try {
			addPage(new ExtractTempInputPage(getExtractTempRefactoring().guessTempName()));
		} catch (JavaModelException e) {
			addPage(new ExtractTempInputPage("")); //$NON-NLS-1$
		}
	}
	
	private ExtractTempRefactoring getExtractTempRefactoring(){
		return (ExtractTempRefactoring)getRefactoring();
	}
	
}
