package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.structure.PullUpRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
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
			setPageTitle(); 
			
			//no input page if there are no methods
			if (JavaElementUtil.getElementsOfType(getPullUpRefactoring().getElementsToPullUp(),  IJavaElement.METHOD).length != 0)
				addPage(new PullUpInputPage());
		} catch (JavaModelException e){
			//log and try anyway
			JavaPlugin.log(e);
			addPage(new PullUpInputPage()); 
		}		
	}

	private void setPageTitle() throws JavaModelException {
		IType initialSetting= getPullUpRefactoring().getDeclaringType();
		IType superType= getPullUpRefactoring().getSuperType(new NullProgressMonitor());
		String title= RefactoringMessages.getFormattedString("PullUpWizard.pageTitleKey", //$NON-NLS-1$
			new String[]{getPageTitle(), 
						 JavaModelUtil.getFullyQualifiedName(initialSetting),
						 JavaModelUtil.getFullyQualifiedName(superType)});
		setPageTitle(title);
	}
	
	private PullUpRefactoring getPullUpRefactoring(){
		return (PullUpRefactoring)getRefactoring();
	}
}