package org.eclipse.jdt.internal.ui.refactoring.actions;
import java.util.Iterator;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.core.refactoring.tagging.IPreactivatedRefactoring;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizardDialog;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IWorkbenchWindow;

abstract class OpenRefactoringWizardAction extends Action{
	
	private Class fActivationType;
	private Refactoring fRefactoring;
	
	OpenRefactoringWizardAction(String label, Class activatedOnType) {
		super(label);
		Assert.isNotNull(activatedOnType);
		fActivationType= activatedOnType;
	}
	
	/**
	 * Tests if the action can be run on the current selection.
	 */
	public boolean canActionBeAdded() {
		IStructuredSelection selection= getCurrentSelection();
		if (selection == null || selection.isEmpty())
			return false;
		return isEnabled(selection);
	}
	
	private IStructuredSelection getCurrentSelection() {
		IWorkbenchWindow window= JavaPlugin.getActiveWorkbenchWindow();
		if (window == null) 
			return null;
		ISelection selection= window.getSelectionService().getSelection();
		if (selection instanceof IStructuredSelection)
				return (IStructuredSelection) selection;
		return null;
	}
	
	private boolean isEnabled(IStructuredSelection selection) {
		for  (Iterator iter= selection.iterator(); iter.hasNext(); ) {
			Object obj= iter.next();
			if (!fActivationType.isInstance(obj) || !shouldAcceptElement(obj))
				return false;
		}
		return true;
	}
	
	/* non java-doc
	 * @see Action#run()
	 */
	public void run() {
		new RefactoringWizardDialog(JavaPlugin.getActiveWorkbenchShell(), createWizard()).open();
	}
	
	private Wizard createWizard(){
		return RefactoringWizardFactory.createWizard(fRefactoring);
	}
	
	/**
	 * Creates a new instance of <code>Refactoring</code>.
	 * @param obj is guaranteed to be of the accepted type (passed in the constructor)
	 */
	protected abstract Refactoring createNewRefactoringInstance(Object obj) throws JavaModelException;	
	
	/**
	 * @param obj is guaranteed to be of the accepted type (passed in the constructor)
	 * @see OpenWizardAction#shouldAcceptElement
	 */
	private final boolean shouldAcceptElement(Object obj) {
		try{
			fRefactoring= createNewRefactoringInstance(obj);
			return canActivateRefactoring();
		} catch (JavaModelException e){
				JavaPlugin.log(e.getStatus());
				return false;
		}	
	}
	
	private boolean canActivateRefactoring()  throws JavaModelException{
		//FIX ME: must have a better solution to this
		if (fRefactoring instanceof IPreactivatedRefactoring)
			return ((IPreactivatedRefactoring)fRefactoring).checkPreactivation().isOK();
		else	
			return fRefactoring.checkActivation(new NullProgressMonitor()).isOK();
	}
}

