/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.reorg;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.StructuredSelectionProvider;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringSupportFactory;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringAction;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
public class RenameAction extends RefactoringAction {
	
	private IRefactoringRenameSupport fRefactoringSupport;
	
	public RenameAction(ISelectionProvider provider) {
		super(ReorgMessages.getString("renameAction.label"), provider); //$NON-NLS-1$
		setDescription(ReorgMessages.getString("renameAction.description")); //$NON-NLS-1$
	}
	
	public RenameAction(StructuredSelectionProvider provider) {
		super(ReorgMessages.getString("renameAction.label"), provider); //$NON-NLS-1$
		setDescription(ReorgMessages.getString("renameAction.description")); //$NON-NLS-1$
	}
	

	/**
	 *The user has invoked this action
	 */
	public void run() {
		try{
			fRefactoringSupport.rename(getStructuredSelection().getFirstElement());
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, ReorgMessages.getString("RenameAction.rename"), ReorgMessages.getString("RenameAction.exception")); //$NON-NLS-1$ //$NON-NLS-2$
		}	
	}
	
	/* non java-doc
	 * @see IRefactoringAction#canOperateOn(IStructuredSelection)
	 */
	public boolean canOperateOn(IStructuredSelection selection) {
		if (selection.size() != 1)
			return false;

		Object element= selection.getFirstElement();
		fRefactoringSupport= RefactoringSupportFactory.createRenameSupport(element);
		if (fRefactoringSupport == null)
			return false;
		try{	
			return fRefactoringSupport.canRename(element);
		} catch (JavaModelException e){
			JavaPlugin.log(e);
			return false;
		}	
	}
		
	String getActionName() {
		return ReorgMessages.getString("renameAction.name"); //$NON-NLS-1$
	}	
}
