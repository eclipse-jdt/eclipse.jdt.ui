/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */
package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.refactoring.RefactoringStatus;
import org.eclipse.jdt.core.refactoring.cus.MoveCompilationUnitRefactoring;

public class MoveCompilationUnitWizard extends RefactoringWizard {
	
	public MoveCompilationUnitWizard(){
		super("Move Compilation Unit");
	}

	/**
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		addPage( new TextInputWizardPage(true) {
			protected RefactoringStatus validatePage() {
				return validateUserInput(getNewName());
			}
			protected String getLabelText(){
				return RefactoringResources.getResourceString("Refactoring.MoveCompilationUnit.wizardpage.labelmessage");
			}	
		});
	}
	
	private IPackageFragmentRoot getPackageFragmentRoot(ICompilationUnit cu){
		return (IPackageFragmentRoot)cu.getParent().getParent();
	}
	
	private RefactoringStatus validateUserInput(String packageName){
		MoveCompilationUnitRefactoring ref= (MoveCompilationUnitRefactoring)getRefactoring();		
		try {
			IPackageFragment pack= getPackage(getPackageFragmentRoot(ref.getCompilationUnit()), packageName);
			if (pack != null){
				ref.setNewPackage(pack);
				return ref.checkPackage();
			} else{
				RefactoringStatus result= new RefactoringStatus();
				result.addFatalError("Package not found");
				return result;
			}	
		} catch (JavaModelException e){
			RefactoringStatus result= new RefactoringStatus();
			result.addFatalError(e.getMessage());
			return result;
		}
	}
	
	/* (non java-doc)
	 * returns null if something is wrong 
	 */ 
	private IPackageFragment getPackage(IPackageFragmentRoot root, String name){
		if (!root.exists()) //can this happen?
			return null;
		return root.getPackageFragment(name);	
	}
}