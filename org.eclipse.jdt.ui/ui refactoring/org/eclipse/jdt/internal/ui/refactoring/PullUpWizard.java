package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.structure.PullUpRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;

public class PullUpWizard extends RefactoringWizard {

	public PullUpWizard(PullUpRefactoring ref, String pageTitle, String errorPageContextHelpId) {
		super(ref, pageTitle, errorPageContextHelpId);
	}
	
	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		try{
			//no input page if there are no methods
			if (JavaElementUtil.getElementsOfType(getPullUpRefactoring().getElementsToPullUp(),  IJavaElement.METHOD).length != 0)
				addPage(new PullUpInputPage());
		} catch (JavaModelException e){
			//log and try anyway
			JavaPlugin.log(e);
			addPage(new PullUpInputPage()); 
		}		
	}
	
	private PullUpRefactoring getPullUpRefactoring(){
		return (PullUpRefactoring)getRefactoring();
	}
}