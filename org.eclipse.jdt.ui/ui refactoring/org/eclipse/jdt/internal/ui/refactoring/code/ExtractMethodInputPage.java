/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000, 2001
 */
package org.eclipse.jdt.internal.ui.refactoring.code;

import org.eclipse.jdt.core.refactoring.RefactoringStatus;
import org.eclipse.jdt.core.refactoring.code.ExtractMethodRefactoring;

import org.eclipse.jdt.internal.ui.refactoring.RefactoringResources;
import org.eclipse.jdt.internal.ui.refactoring.TextInputWizardPage;

public class ExtractMethodInputPage extends TextInputWizardPage {

	private static final String PREFIX= "ExtractMethod.inputPage.";

	public ExtractMethodInputPage() {
		super(true);
		setDescription(RefactoringResources.getResourceString(PREFIX + "description"));
	}
	
	protected String getLabelText(){
		return RefactoringResources.getResourceString(PREFIX + "newName.message");
	}
	
	protected String getInitialValue() {
		ExtractMethodRefactoring refactoring= (ExtractMethodRefactoring)getRefactoring();
		return refactoring.getMethodName();
	}	

	protected RefactoringStatus validatePage() {
		String res= null;
		ExtractMethodRefactoring refactoring= (ExtractMethodRefactoring)getRefactoring();
		refactoring.setMethodName(getNewName());
		return refactoring.checkMethodName();
	}	
}