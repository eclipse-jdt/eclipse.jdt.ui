/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.refactoring;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.core.refactoring.RefactoringStatus;import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;import org.eclipse.jdt.internal.ui.refactoring.RenameInputWizardPage;import org.eclipse.jdt.internal.ui.refactoring.TextInputWizardPage;import org.eclipse.jdt.core.refactoring.methods.RenameParametersRefactoring;

public class RenameParametersWizard extends RefactoringWizard {
	
	public RenameParametersWizard(String name){
		super(name);
	}

	/**
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		addPage(new RenameParametersWizardPage(true));
	}
}