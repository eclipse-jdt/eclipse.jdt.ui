package org.eclipse.jdt.internal.ui.refactoring.actions;

import java.util.Iterator;

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;
import org.eclipse.jdt.ui.actions.UnifiedSite;

import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public abstract class OpenRefactoringWizardAction extends SelectionDispatchAction {

	private Class fActivationType;
	private Refactoring fRefactoring;
	
	protected OpenRefactoringWizardAction(String label, UnifiedSite site, Class activatedOnType) {
		super(site);
		setText(label);
		fActivationType= activatedOnType;
	}

	/*
	 * @see SelectionDispatchAction#selectionChanged(IStructuredSelection)
	 */
	protected void selectionChanged(IStructuredSelection selection) {
		setEnabled(canOperateOn(selection));
	}

	protected boolean canOperateOn(IStructuredSelection selection){
		if (selection.isEmpty())
			return canOperateOnEmptySelection();
		
		if (selection.size() > 1 && !canOperateOnMultiSelection())	
			return false;
		
		if (selection.size() == 1&& !canOperateOnMultiSelection())
			return fActivationType.isInstance(selection.getFirstElement()) && shouldAcceptElement(selection.getFirstElement());
				
		for  (Iterator iter= selection.iterator(); iter.hasNext(); ) {
			if (!fActivationType.isInstance(iter.next()))
				return false;
		}
		return shouldAcceptElement(selection.toArray());
	}

	protected boolean canOperateOnMultiSelection(){
		return false;
	}
	
	protected boolean canOperateOnEmptySelection(){
		return false;
	}
		
	/*
	 * @see SelectionDispatchAction#run(IStructuredSelection)
	 */
	public void run(IStructuredSelection selection) {
		Assert.isNotNull(fRefactoring);
		try{
			new RefactoringStarter().activate(fRefactoring, createWizard(fRefactoring), RefactoringMessages.getString("OpenRefactoringWizardAction.refactoring"), true); //$NON-NLS-1$
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, RefactoringMessages.getString("OpenRefactoringWizardAction.refactoring"), RefactoringMessages.getString("OpenRefactoringWizardAction.exception")); //$NON-NLS-1$ //$NON-NLS-2$
		}	
	}
		
	/**
	 * Creates a new instance of <code>Refactoring</code>.
	 * @param obj is guaranteed to be of the accepted type (passed in the constructor)
	 * However, if <code>canOperateOnMultiSelection</code> resturns <code>true</code>, 
	 * then obj is <code>Object[]</code> and contains elements of the accepted type (passed in the constructor) 
	 */
	protected abstract Refactoring createNewRefactoringInstance(Object obj) throws JavaModelException;	
	
	protected abstract RefactoringWizard createWizard(Refactoring refactoring);	
	
	/**
	 * @param obj is guaranteed to be of the accepted type (passed in the constructor)
	 * However, if <code>canOperateOnMultiSelection</code> resturns <code>true</code>, 
	 * then obj is <code>Object[]</code> and contains elements of the accepted type (passed in the constructor) 
	 * @see OpenWizardAction#shouldAcceptElement
	 */
	private final boolean shouldAcceptElement(Object obj) {
		try{
			fRefactoring= createNewRefactoringInstance(obj);
			return canActivateRefactoring(fRefactoring);
		} catch (JavaModelException e){
				JavaPlugin.log(e.getStatus());
				return false;
		}	
	}
	
	protected abstract boolean canActivateRefactoring(Refactoring refactoring)  throws JavaModelException;
}
