/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer;
import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ClasspathContainerWizard;

/**
 * Action to open a dialog to configure classpath containers. Added as a <code>objectContribution</code>
 * to {@link ClassPathContainer}.
 */
public class ConfigureContainerAction implements IObjectActionDelegate {

	private ISelection fCurrentSelection;
	private IWorkbenchPart fPart;

	/*
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		fPart= targetPart;
	}

	/*
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		if (fCurrentSelection instanceof IStructuredSelection) {
			ClassPathContainer container= (ClassPathContainer) ((IStructuredSelection) fCurrentSelection).getFirstElement();
			openWizard(container.getClasspathEntry(), container.getLabel(container), container.getJavaProject());
		}
	}
	
	private void openWizard(IClasspathEntry entry, String label, final IJavaProject project) {
		Shell shell= fPart.getSite().getShell();
		try {
			IClasspathEntry[] entries= project.getRawClasspath();
			
			ClasspathContainerWizard wizard= new ClasspathContainerWizard(entry, project, entries);
			wizard.setWindowTitle(ActionMessages.getFormattedString("ConfigureContainerAction.wizard.title", label)); //$NON-NLS-1$
			if (ClasspathContainerWizard.openWizard(shell, wizard) != Window.OK) {
				return;
			}
			IRunnableContext context= fPart.getSite().getWorkbenchWindow();
			if (context == null) {
				context= new BusyIndicatorRunnableContext();
			}
			IClasspathEntry[] res= wizard.getNewEntries();
			if (res == null) {
				String title= ActionMessages.getString("ConfigureContainerAction.error.title"); //$NON-NLS-1$
				String message= ActionMessages.getString("ConfigureContainerAction.error.creationfailed.message"); //$NON-NLS-1$
				MessageDialog.openError(shell, title, message);
				return;
			}
			if (res.length == 1 && res[0].equals(entry)) {
				return; // no changes
			}
			
			int idx= indexInClasspath(entries, entry);
			if (idx == -1) {
				return;
			}
			
			final IClasspathEntry[] newEntries= new IClasspathEntry[entries.length - 1 + res.length];
			System.arraycopy(entries, 0, newEntries, 0, idx);
			System.arraycopy(res, 0, newEntries, idx, res.length);
			System.arraycopy(entries, idx + 1, newEntries, idx + res.length, entries.length - idx - 1);
			
			context.run(true, true, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {			
						project.setRawClasspath(newEntries, project.getOutputLocation(), monitor);
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				}
			});
		} catch (JavaModelException e) {
			String title= ActionMessages.getString("ConfigureContainerAction.error.title"); //$NON-NLS-1$
			String message= ActionMessages.getString("ConfigureContainerAction.error.creationfailed.message"); //$NON-NLS-1$
			ExceptionHandler.handle(e, shell, title, message);
		} catch (InvocationTargetException e) {
			String title= ActionMessages.getString("ConfigureContainerAction.error.title"); //$NON-NLS-1$
			String message= ActionMessages.getString("ConfigureContainerAction.error.applyingfailed.message"); //$NON-NLS-1$
			ExceptionHandler.handle(e, shell, title, message);
		} catch (InterruptedException e) {
			// user cancelled
		}
	}
	
	protected static int indexInClasspath(IClasspathEntry[] entries, IClasspathEntry entry) {
		for (int i= 0; i < entries.length; i++) {
			if (entries[i] == entry) {
				return i;
			}
		}
		return -1;
	}

	/*
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		fCurrentSelection= selection;
	}

}
