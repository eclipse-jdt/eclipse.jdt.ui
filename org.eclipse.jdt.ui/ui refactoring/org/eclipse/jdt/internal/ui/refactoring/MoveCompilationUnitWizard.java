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

	private static final String PAGE_TITLE= "Move Compilation Unit";
	public MoveCompilationUnitWizard(){
		super(PAGE_TITLE);
	}

	/**
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		MoveCompilationUnitRefactoring ref= getMoveCompilationUnitRefactoring();
		setPageTitle(getPageTitle() + ": " + ref.getCompilationUnit().getElementName());
		addPage( new MoveCompilationUnitWizardPage(true){
			protected RefactoringStatus validateTextField(String text) {
				return validateUserInput(text);
			}	
		}
		);
	}
	
	private MoveCompilationUnitRefactoring getMoveCompilationUnitRefactoring(){
		return (MoveCompilationUnitRefactoring)getRefactoring();
	}
	
	private IPackageFragmentRoot getPackageFragmentRoot(ICompilationUnit cu){
		return (IPackageFragmentRoot)cu.getParent().getParent();
	}
	
	private RefactoringStatus validateUserInput(String packageName){
		MoveCompilationUnitRefactoring ref= getMoveCompilationUnitRefactoring();
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