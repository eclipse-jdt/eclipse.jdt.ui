/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.reorg;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.MultiStatus;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.reorg.DeleteRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgUtils;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIException;
import org.eclipse.jdt.internal.ui.actions.StructuredSelectionProvider;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.actions.DeleteResourceAction;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jdt.internal.corext.refactoring.reorg.*;

/** 
 * Action for deleting elements in a delete target.
 */
class JdtDeleteResourceAction extends ReorgAction {
	private boolean fDeleteProjectContent;

	public JdtDeleteResourceAction(ISelectionProvider provider) {
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
		
		fDeleteProjectContent= false;
		if (!confirmDelete())
			return;

		if (hasReadOnlyResources() && !isOkToDeleteReadOnly()) 
			return;
		refactoring.setDeleteProjectContents(fDeleteProjectContent);
		try{
			MultiStatus status= ReorgAction.perform(refactoring);
			if (!status.isOK()) {
				JavaUIException t= new JavaUIException(status);
				ExceptionHandler.handle(t, "Delete", "Unexpected exception. See log for details.");
			}
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, "Delete", "Unexpected exception. See log for details.");
			return;
		}	
	}
	
	private boolean hasOnlyProjects(){
		return (! getStructuredSelection().isEmpty() && getStructuredSelection().size() == getProjects().size());
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
		Assert.isTrue(getProjects().isEmpty());
		String title= ReorgMessages.getString("deleteAction.confirm.title"); //$NON-NLS-1$
		String label= ReorgMessages.getString("deleteAction.confirm.message"); //$NON-NLS-1$
		Shell parent= JavaPlugin.getActiveWorkbenchShell();
		return MessageDialog.openConfirm(parent, title, label);
	}
	
	private List getProjects() {
		List result= new ArrayList(getStructuredSelection().size());
		for(Iterator iter= getStructuredSelection().iterator(); iter.hasNext(); ) {
			Object element= iter.next();
			if (element instanceof IJavaProject) {
				try {
					result.add(((IJavaProject)element).getUnderlyingResource());
				} catch (JavaModelException e) {
					if (!e.isDoesNotExist()) {
						//do not show error dialogs in a loop
						JavaPlugin.log(e);
					}
				}
			}
		}
		return result;
	}	
}
