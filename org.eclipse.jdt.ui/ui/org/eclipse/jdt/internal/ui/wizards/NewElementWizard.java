/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards;

import java.lang.reflect.InvocationTargetException;import org.eclipse.swt.widgets.Display;import org.eclipse.swt.widgets.Shell;import org.eclipse.core.resources.IFile;import org.eclipse.core.resources.IResource;import org.eclipse.jface.dialogs.MessageDialog;import org.eclipse.jface.operation.IRunnableWithProgress;import org.eclipse.jface.viewers.ISelection;import org.eclipse.jface.viewers.IStructuredSelection;import org.eclipse.jface.viewers.StructuredSelection;import org.eclipse.ui.INewWizard;import org.eclipse.ui.IWorkbench;import org.eclipse.ui.IWorkbenchPage;import org.eclipse.ui.IWorkbenchPart;import org.eclipse.ui.IWorkbenchWindow;import org.eclipse.ui.PartInitException;import org.eclipse.ui.actions.WorkspaceModifyDelegatingOperation;import org.eclipse.ui.part.ISetSelectionTarget;import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public abstract class NewElementWizard extends BasicNewResourceWizard implements INewWizard {

	public NewElementWizard() {
		setNeedsProgressMonitor(true);
	}
	
	/**
	 * @see BasicNewResourceWizard#initializeDefaultPageImageDescriptor
	 */
	protected void initializeDefaultPageImageDescriptor() {
		// no action, we do not need the desktop default
	}
	
		
	protected void openResource(final IResource resource) {
		if (resource.getType() == IResource.FILE) {
			final IWorkbenchPage activePage= JavaPlugin.getDefault().getActivePage();
			if (activePage != null) {
				final Display display= getShell().getDisplay();
				if (display != null) {
					display.asyncExec(new Runnable() {
						public void run() {
							try {
								activePage.openEditor((IFile)resource);
							} catch (PartInitException e) {
								JavaPlugin.log(e);
							}
						}
					});
				}
			}
		}
	}		
	
	/**
	 * Run a runnbable
	 */	
	public boolean finishPage(IRunnableWithProgress runnable) {
		IRunnableWithProgress op= new WorkspaceModifyDelegatingOperation(runnable);
		try {
			getContainer().run(false, true, op);
		} catch (InvocationTargetException e) {
			Shell shell= getShell();
			String title= NewWizardMessages.getString("NewElementWizard.op_error.title"); //$NON-NLS-1$
			String message= NewWizardMessages.getString("NewElementWizard.op_error.message"); //$NON-NLS-1$
			if (!ExceptionHandler.handle(e.getTargetException(), shell, title, message)) {
				MessageDialog.openError(shell, title, e.getTargetException().getMessage());
			}
			return false;
		} catch  (InterruptedException e) {
			return false;
		}
		return true;
	}	
	
	
}