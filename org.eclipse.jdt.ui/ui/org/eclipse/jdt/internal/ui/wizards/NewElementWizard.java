/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;

import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

import org.eclipse.jdt.internal.corext.template.java.Templates;
import org.eclipse.jdt.internal.ui.IUIConstants;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jdt.internal.ui.dialogs.OptionalMessageDialog;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public abstract class NewElementWizard extends Wizard implements INewWizard {

	private IWorkbench fWorkbench;
	private IStructuredSelection fSelection;

	public NewElementWizard() {
		setNeedsProgressMonitor(true);
	}
			
	protected void openResource(final IFile resource) {
		final IWorkbenchPage activePage= JavaPlugin.getActivePage();
		if (activePage != null) {
			final Display display= getShell().getDisplay();
			if (display != null) {
				display.asyncExec(new Runnable() {
					public void run() {
						try {
							IDE.openEditor(activePage, resource, true);
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
	 * This method is run in the wizard container's context as a workspace runnable.
	 * @param monitor
	 * @throws InterruptedException
	 * @throws CoreException
	 */
	protected abstract void finishPage(IProgressMonitor monitor) throws InterruptedException, CoreException;
	
	/**
	 * Returns the scheduling rule for creating the element.
	 */
	protected abstract ISchedulingRule getSchedulingRule();
	
	
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
			getContainer().run(false, true, new WorkbenchRunnableAdapter(op, getSchedulingRule()));
		} catch (InvocationTargetException e) {
			handleFinishException(getShell(), e);
			return false;
		} catch  (InterruptedException e) {
			return false;
		}
		return true;
	}
	
	protected void warnAboutTypeCommentDeprecation() {
		String key= IUIConstants.DIALOGSTORE_TYPECOMMENT_DEPRECATED;
		if (OptionalMessageDialog.isDialogEnabled(key)) {
			Templates templates= Templates.getInstance();
			boolean isOldWorkspace= templates.getTemplates("filecomment").length > 0 && templates.getTemplates("typecomment").length > 0; //$NON-NLS-1$ //$NON-NLS-2$
			if (!isOldWorkspace) {
				OptionalMessageDialog.setDialogEnabled(key, false);
			}
			String title= NewWizardMessages.getString("NewElementWizard.typecomment.deprecated.title"); //$NON-NLS-1$
			String message= NewWizardMessages.getString("NewElementWizard.typecomment.deprecated.message"); //$NON-NLS-1$
			OptionalMessageDialog.open(key, getShell(), title, null, message, MessageDialog.INFORMATION, new String[] { IDialogConstants.OK_LABEL }, 0);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
		fWorkbench= workbench;
		fSelection= currentSelection;
	}
	
	public IStructuredSelection getSelection() {
		return fSelection;
	}

	public IWorkbench getWorkbench() {
		return fWorkbench;
	}

	protected void selectAndReveal(IResource newResource) {
		BasicNewResourceWizard.selectAndReveal(newResource, fWorkbench.getActiveWorkbenchWindow());
	}

}
