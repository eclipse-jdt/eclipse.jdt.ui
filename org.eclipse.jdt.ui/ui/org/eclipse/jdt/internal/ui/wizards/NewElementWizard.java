/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.WorkspaceModifyDelegatingOperation;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public abstract class NewElementWizard extends BasicNewResourceWizard implements INewWizard {

	public NewElementWizard() {
		setNeedsProgressMonitor(true);
	}
	
	/*
	 * @see BasicNewResourceWizard#initializeDefaultPageImageDescriptor
	 */
	protected void initializeDefaultPageImageDescriptor() {
		// no action, we do not need the desktop default
	}
	
		
	protected void openResource(final IFile resource) {
		final IWorkbenchPage activePage= JavaPlugin.getActivePage();
		if (activePage != null) {
			final Display display= getShell().getDisplay();
			if (display != null) {
				display.asyncExec(new Runnable() {
					public void run() {
						try {
							activePage.openEditor(resource);
						} catch (PartInitException e) {
							JavaPlugin.log(e);
						}
					}
				});
			}
		}
	}
	
	/**
	 * Subclasses should override to perform the actions of the wizard.
	 * This method is run in the wizard container's context as a workspace runnable.	 */
	protected void finishPage(IProgressMonitor monitor) throws InterruptedException, CoreException {
	}
	
	protected void handleFinishException(Shell shell, InvocationTargetException e) {
		String title= NewWizardMessages.getString("NewElementWizard.op_error.title"); //$NON-NLS-1$
		String message= NewWizardMessages.getString("NewElementWizard.op_error.message"); //$NON-NLS-1$
		ExceptionHandler.handle(e, shell, title, message);
	}
	
	/*
	 * @see Wizard#performFinish
	 */		
	public boolean performFinish() {
		IWorkspaceRunnable op= new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException, OperationCanceledException {
				try {
					finishPage(monitor);
				} catch (InterruptedException e) {
					throw new OperationCanceledException(e.getMessage());
				}
			}
		};
		try {
			getContainer().run(false, true, new WorkbenchRunnableAdapter(op));
		} catch (InvocationTargetException e) {
			handleFinishException(getShell(), e);
			return false;
		} catch  (InterruptedException e) {
			return false;
		}
		return true;
	}	
	
	
}