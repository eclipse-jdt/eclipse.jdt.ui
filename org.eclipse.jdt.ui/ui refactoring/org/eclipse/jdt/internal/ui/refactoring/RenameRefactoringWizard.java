/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */
package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jdt.core.refactoring.RefactoringStatus;
import org.eclipse.jdt.core.refactoring.tagging.IRenameRefactoring;import org.eclipse.jdt.internal.core.refactoring.DebugUtils;

public class RenameRefactoringWizard extends RefactoringWizard {
	
	public RenameRefactoringWizard(String name){
		super(name);
	}

	/**
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		String initialSetting= ((IRenameRefactoring)getRefactoring()).getCurrentName();
		addPage( new TextInputWizardPage(true, initialSetting) {
			protected RefactoringStatus validatePage() {
				return validateNewName(getNewName());
			}	
		});
	}
	
	private RefactoringStatus validateNewName(String newName){
		IRenameRefactoring ref= (IRenameRefactoring)getRefactoring();		
		ref.setNewName(newName);
		return ref.checkNewName();
	}
}