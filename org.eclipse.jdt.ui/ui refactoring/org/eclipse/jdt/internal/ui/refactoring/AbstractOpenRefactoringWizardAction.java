/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */
package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.refactoring.Refactoring;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.AbstractOpenWizardAction;

public abstract class AbstractOpenRefactoringWizardAction extends AbstractOpenWizardAction {

	private Refactoring fRefactoring;
	private static final IProgressMonitor fgNullProgressMonitor= new NullProgressMonitor();
	
	public AbstractOpenRefactoringWizardAction(ISelectionProvider viewer, String label, Class activatedOnType) {
		super(JavaPlugin.getDefault().getWorkbench(), label, new Class[] {activatedOnType}, false);
	}
	
	/**
	 * Creates a new instance of <code>Refactoring</code>.
	 * @param obj is guaranteed to be of the accepted type (passed in the constructor)
	 */
	protected abstract Refactoring createNewRefactoringInstance(Object obj);	
	
	private boolean canActivateRefactoring() {
		try {
			return fRefactoring.checkActivation(fgNullProgressMonitor).isOK();
		} catch (JavaModelException e){
			return false;
		}
	}
	
	private RefactoringWizard createRefactoringWizard(){
		Assert.isNotNull(fRefactoring);// sanity check, refactoring should be set in shouldAcceptElement
		RefactoringWizard wizard= (RefactoringWizard)createWizard();
		wizard.init(fRefactoring);
		return wizard;
	}
	
	/**
     * The user has invoked this action.
     */
	public void run() {
		WizardDialog dialog= new RefactoringWizardDialog(JavaPlugin.getActiveWorkbenchShell(), createRefactoringWizard());
		dialog.open();
	}
	
	/**
	 * @param obj is guaranteed to be of the accepted type (passed in the constructor)
	 * @see OpenWizardAction#shouldAcceptElement
	 */
	protected final boolean shouldAcceptElement(Object obj) {
		fRefactoring= createNewRefactoringInstance(obj);
		return canActivateRefactoring();
	}
}
