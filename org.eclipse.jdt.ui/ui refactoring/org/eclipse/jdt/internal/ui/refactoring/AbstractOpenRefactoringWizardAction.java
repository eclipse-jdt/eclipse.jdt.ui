/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;

import org.eclipse.jdt.internal.core.refactoring.tagging.IPreactivatedRefactoring;import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.AbstractOpenWizardAction;

public abstract class AbstractOpenRefactoringWizardAction extends AbstractOpenWizardAction {

	private Refactoring fRefactoring;
	
	public AbstractOpenRefactoringWizardAction(String label, Class activatedOnType) {
		super(label, new Class[] {activatedOnType}, false);
	}
	
	/**
	 * Creates a new instance of <code>Refactoring</code>.
	 * @param obj is guaranteed to be of the accepted type (passed in the constructor)
	 */
	protected abstract Refactoring createNewRefactoringInstance(Object obj);	
	
	private boolean canActivateRefactoring() {
		try {
			//FIX ME: must have a better solution to this
			if (fRefactoring instanceof IPreactivatedRefactoring)
				return ((IPreactivatedRefactoring)fRefactoring).checkPreactivation().isOK();
			else	
				return fRefactoring.checkActivation(new NullProgressMonitor()).isOK();
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
