/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.actions;

import java.util.Iterator;

import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.StructuredSelectionProvider;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public abstract class OpenRefactoringWizardAction extends RefactoringAction {
	
	private Class fActivationType;
	private Refactoring fRefactoring;
	
	public OpenRefactoringWizardAction(String label, StructuredSelectionProvider provider, Class activatedOnType) {
		super(label, provider);
		Assert.isNotNull(activatedOnType);
		fActivationType= activatedOnType;
	}
	
	/* non java-doc
	 * @see IRefactoringAction#canOperateOn(IStructuredSelection)
	 */
	public boolean canOperateOn(IStructuredSelection selection){
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
		
	/* non java-doc
	 * @see Action#run()
	 */
	public void run() {
		Assert.isNotNull(fRefactoring);
		try{
			new RefactoringStarter().activate(fRefactoring, createWizard(fRefactoring), "Refactoring", true);
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, "Refactoring", "Unexpected exception occurred. See log for details.");
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
	
	abstract boolean canActivateRefactoring(Refactoring refactoring)  throws JavaModelException;
}

