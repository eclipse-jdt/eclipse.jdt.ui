/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.reorg;

import java.util.Iterator;

import org.eclipse.core.runtime.MultiStatus;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.actions.DeleteResourceAction;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.reorg.DeleteRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgUtils;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/** 
 * Action for deleting elements in a delete target.
 */
class DeleteResourcesAction extends ReorgAction {
	public DeleteResourcesAction(ISelectionProvider provider) {
		super(ReorgMessages.getString("deleteAction.label"), provider); //$NON-NLS-1$
		setDescription(ReorgMessages.getString("deleteAction.description")); //$NON-NLS-1$
	}

	/**
	 * The user has invoked this action
	 */
	public void run() {
		if (hasOnlyProjects()){
			deleteProjects();
			return;
		}	

		DeleteRefactoring refactoring= new DeleteRefactoring(getStructuredSelection().toList());
		
		if (!confirmDelete())
			return;

		if (hasReadOnlyResources() && !isOkToDeleteReadOnly()) 
			return;
		try{
			MultiStatus status= ReorgAction.perform(refactoring);
			if (!status.isOK()) {
				JavaPlugin.log(status);
				ErrorDialog.openError(JavaPlugin.getActiveWorkbenchShell(), ReorgMessages.getString("DeleteResourceAction.delete"), ReorgMessages.getString("DeleteResourceAction.exception"), status); //$NON-NLS-1$ //$NON-NLS-2$
			}
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, ReorgMessages.getString("DeleteResourceAction.delete"), ReorgMessages.getString("DeleteResourceAction.exception")); //$NON-NLS-1$ //$NON-NLS-2$
			return;
		}	
	}
		
	private void deleteProjects(){
		DeleteResourceAction action= new DeleteResourceAction(JavaPlugin.getActiveWorkbenchShell());
		action.selectionChanged(getStructuredSelection());
		action.run();
	}
	
	private static boolean isOkToDeleteReadOnly(){
			String msg= ReorgMessages.getString("deleteAction.confirmReadOnly"); //$NON-NLS-1$
			String title= ReorgMessages.getString("deleteAction.checkDeletion"); //$NON-NLS-1$
			return MessageDialog.openQuestion(
					JavaPlugin.getActiveWorkbenchShell(),
					title,
					msg);
	}
	
	private boolean hasReadOnlyResources(){
		for (Iterator iter= getStructuredSelection().iterator(); iter.hasNext();){	
			if (ReorgUtils.shouldConfirmReadOnly(iter.next()))
				return true;
		}
		return false;
	}

	/* non java-doc
	 * @see IRefactoringAction#canOperateOn(IStructuredSelection)
	 */
	public boolean canOperateOn(IStructuredSelection selection) {
		return ReorgAction.canActivate(new DeleteRefactoring(selection.toList()));
	}
	
	private boolean confirmDelete() {
		Assert.isTrue(getSelectedProjects().isEmpty());
		String title= ReorgMessages.getString("deleteAction.confirm.title"); //$NON-NLS-1$
		String label= ReorgMessages.getString("deleteAction.confirm.message"); //$NON-NLS-1$
		Shell parent= JavaPlugin.getActiveWorkbenchShell();
		return MessageDialog.openQuestion(parent, title, label);
	}
}
