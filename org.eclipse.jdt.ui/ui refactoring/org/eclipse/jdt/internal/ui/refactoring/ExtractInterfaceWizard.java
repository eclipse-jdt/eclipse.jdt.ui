/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */
package org.eclipse.jdt.internal.ui.refactoring;

public class ExtractInterfaceWizard extends RefactoringWizard {

	public ExtractInterfaceWizard() {
		super("Refactoring.ExtractInterface");
	}
	
	/**
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		addPage(new ExtractInterfaceWizardPage());
	}
}