package org.eclipse.jdt.internal.ui.reorg;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.actions.DeleteResourceAction;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;
import org.eclipse.jdt.ui.actions.UnifiedSite;

import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.reorg.DeleteRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgUtils;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.refactoring.CreateChangeOperation;
import org.eclipse.jdt.internal.ui.refactoring.PerformChangeOperation;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class DeleteResourcesAction extends SelectionDispatchAction {

	protected DeleteResourcesAction(UnifiedSite site) {
		super(site);
	}

	/*
	 * @see SelectionDispatchAction#run(IStructuredSelection)
	 */
	protected void run(IStructuredSelection selection) {
		if (ClipboardActionUtil.hasOnlyProjects(selection)){
			deleteProjects(selection);
			return;
		}	

		DeleteRefactoring refactoring= new DeleteRefactoring(selection.toList());
		
		if (!confirmDelete(selection))
			return;

		if (hasReadOnlyResources(selection) && !isOkToDeleteReadOnly()) 
			return;
		try{
			MultiStatus status= ClipboardActionUtil.perform(refactoring);
			if (!status.isOK()) {
				JavaPlugin.log(status);
				ErrorDialog.openError(JavaPlugin.getActiveWorkbenchShell(), ReorgMessages.getString("DeleteResourceAction.delete"), ReorgMessages.getString("DeleteResourceAction.exception"), status); //$NON-NLS-1$ //$NON-NLS-2$
			}
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, ReorgMessages.getString("DeleteResourceAction.delete"), ReorgMessages.getString("DeleteResourceAction.exception")); //$NON-NLS-1$ //$NON-NLS-2$
			return;
		}	
	}

	private void deleteProjects(IStructuredSelection selection){
		DeleteResourceAction action= new DeleteResourceAction(JavaPlugin.getActiveWorkbenchShell());
		action.selectionChanged(selection);
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
	
	private boolean hasReadOnlyResources(IStructuredSelection selection){
		for (Iterator iter= selection.iterator(); iter.hasNext();){	
			if (ReorgUtils.shouldConfirmReadOnly(iter.next()))
				return true;
		}
		return false;
	}
	
	/*
	 * @see SelectionDispatchAction#selectionChanged(IStructuredSelection)
	 */
	protected void selectionChanged(IStructuredSelection selection) {
		setEnabled(ClipboardActionUtil.canActivate(new DeleteRefactoring(selection.toList())));
	}
	
	private boolean confirmDelete(IStructuredSelection selection) {
		Assert.isTrue(ClipboardActionUtil.getSelectedProjects(selection).isEmpty());
		String title= ReorgMessages.getString("deleteAction.confirm.title"); //$NON-NLS-1$
		String label= ReorgMessages.getString("deleteAction.confirm.message"); //$NON-NLS-1$
		Shell parent= JavaPlugin.getActiveWorkbenchShell();
		return MessageDialog.openQuestion(parent, title, label);
	}

		
	
	
}
