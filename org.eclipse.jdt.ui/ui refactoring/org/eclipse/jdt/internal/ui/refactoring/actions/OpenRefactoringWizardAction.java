/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.actions;

import java.util.Iterator;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizardDialog;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jdt.internal.ui.actions.*;

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
			
		for  (Iterator iter= selection.iterator(); iter.hasNext(); ) {
			Object obj= iter.next();
			if (!fActivationType.isInstance(obj) || !shouldAcceptElement(obj))
				return false;
		}
		return true;
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
			activateRefactoringWizard(fRefactoring, createWizard(fRefactoring), "Refactoring");
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, "Refactoring", "Unexpected exception occurred. See log for details.");
		}	
	}
		
	/**
	 * Creates a new instance of <code>Refactoring</code>.
	 * @param obj is guaranteed to be of the accepted type (passed in the constructor)
	 */
	protected abstract Refactoring createNewRefactoringInstance(Object obj) throws JavaModelException;	
	
	protected abstract RefactoringWizard createWizard(Refactoring refactoring);	
	
	/**
	 * @param obj is guaranteed to be of the accepted type (passed in the constructor)
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

