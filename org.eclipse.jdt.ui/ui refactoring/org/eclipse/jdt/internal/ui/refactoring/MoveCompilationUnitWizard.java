/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.core.refactoring.cus.MoveCompilationUnitRefactoring;import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;

public class MoveCompilationUnitWizard extends RefactoringWizard {

	public MoveCompilationUnitWizard(){
		super(RefactoringMessages.getString("MoveCompilationUnitWizard.title"), IJavaHelpContextIds.MOVE_CU_ERROR_WIZARD_PAGE); //$NON-NLS-1$
	}

	/**
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		MoveCompilationUnitRefactoring ref= getMoveCompilationUnitRefactoring();
		setPageTitle(getPageTitle() + ": " + ref.getCompilationUnit().getElementName()); //$NON-NLS-1$
		MoveCompilationUnitWizardPage page=  new MoveCompilationUnitWizardPage(true){
			protected RefactoringStatus validateTextField(String text) {
				return validateUserInput(text);
			}	
		};
		page.setMessage(RefactoringMessages.getString("MoveCompilationUnitWizard.message")); //$NON-NLS-1$
		addPage(page);
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
				result.addFatalError(RefactoringMessages.getString("MoveCompilationUnitWizard.package_not_found")); //$NON-NLS-1$
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