/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.nls;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSRefactoring;
import org.eclipse.jdt.internal.ui.refactoring.nls.search.ExceptionHandler;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringAction;


public class ExternalizeAction implements IWorkbenchWindowActionDelegate{

	protected static Refactoring createNewRefactoringInstance(ICompilationUnit cu) {
		return new NLSRefactoring(cu);
	}
	
	/*
	 * @see IWorkbenchWindowActionDelegate#dispose()
	 */
	public void dispose() {
	}

	/*
	 * @see IWorkbenchWindowActionDelegate#init(IWorkbenchWindow)
	 */
	public void init(IWorkbenchWindow window) {
	}
	
	protected IStructuredSelection getSelection() {
		IWorkbenchWindow window= JavaPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow();
		if (window == null)
			return null;
			
		ISelection selection= window.getSelectionService().getSelection();
		if (selection instanceof IStructuredSelection) 
			return (IStructuredSelection) selection;			
		return null;
	}
	
	protected ICompilationUnit getCompilationUnit(IStructuredSelection selection) {
		if (selection == null)
			return null;
			
		Object first= selection.getFirstElement();
		if (first instanceof ICompilationUnit) 
			return (ICompilationUnit) first;
		
		return null;
	}
	
	/*
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		try{
			ICompilationUnit unit= getCompilationUnit(getSelection());
			if (unit == null)
				return;
	
			Refactoring refactoring= createNewRefactoringInstance(unit);
			ExternalizeWizard wizard= new ExternalizeWizard(refactoring);
			String title= NLSUIMessages.getString("action.name"); //$NON-NLS-1$
			RefactoringAction.activateRefactoringWizard(refactoring, wizard, title, true);
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, "Externalize Strings", "Unexpected exception. See log for details.");
		}
	}

	/*
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
	}
}
