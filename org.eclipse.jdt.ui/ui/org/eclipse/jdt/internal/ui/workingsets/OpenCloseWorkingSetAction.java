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
package org.eclipse.jdt.internal.ui.workingsets;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

public abstract class OpenCloseWorkingSetAction extends SelectionDispatchAction implements IResourceChangeListener {

	private OpenCloseWorkingSetAction(IWorkbenchSite site, String label) {
		super(site);
		setText(label);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
	}
	
	public static OpenCloseWorkingSetAction createCloseAction(IWorkbenchSite site) {
		return new OpenCloseWorkingSetAction(site, WorkingSetMessages.getString("OpenCloseWorkingSetAction.close.label")) { //$NON-NLS-1$
			protected boolean validate(IProject project) {
				return project.isOpen();
			}
 			protected void performOperation(IProject project, IProgressMonitor monitor) throws CoreException {
				project.close(monitor);
			}
			protected String getErrorTitle() {
				return WorkingSetMessages.getString("OpenCloseWorkingSetAction.close.error.title"); //$NON-NLS-1$
			}
			protected String getErrorMessage() {
				return WorkingSetMessages.getString("OpenCloseWorkingSetAction.close.error.message"); //$NON-NLS-1$
			}
		};
	}

	public static OpenCloseWorkingSetAction createOpenAction(IWorkbenchSite site) {
		return new OpenCloseWorkingSetAction(site, WorkingSetMessages.getString("OpenCloseWorkingSetAction.open.label")) { //$NON-NLS-1$
			protected boolean validate(IProject project) {
				return !project.isOpen();
			}
 			protected void performOperation(IProject project, IProgressMonitor monitor) throws CoreException {
				project.open(monitor);
			}
			protected String getErrorTitle() {
				return WorkingSetMessages.getString("OpenCloseWorkingSetAction.open.error.title"); //$NON-NLS-1$
			}
			protected String getErrorMessage() {
				return WorkingSetMessages.getString("OpenCloseWorkingSetAction.open.error.message"); //$NON-NLS-1$
			}
		};
	}
	
	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
	}

	public void selectionChanged(IStructuredSelection selection) {
		setEnabled(getProjects(selection) != null);
	}
	
	public void run(IStructuredSelection selection) {
		final List projects= getProjects(selection);
		if (projects == null)
			return;
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(
				new WorkbenchRunnableAdapter(new IWorkspaceRunnable() {
					public void run(IProgressMonitor monitor) throws CoreException {
						monitor.beginTask("", projects.size()); //$NON-NLS-1$
						for (Iterator iter= projects.iterator(); iter.hasNext();) {
							IProject project= (IProject)iter.next();
							performOperation(project, new SubProgressMonitor(monitor, 1));
						}
						monitor.done();
					}
				}));
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, getShell(), getErrorTitle(), getErrorMessage());
		} catch (InterruptedException e) {
			// do nothing. Got cancelled.
		}
	}
	
	protected abstract boolean validate(IProject project);
	
	protected abstract void performOperation(IProject project, IProgressMonitor monitor) throws CoreException;
	
	protected abstract String getErrorTitle();

	protected abstract String getErrorMessage();
	
	private List getProjects(IStructuredSelection selection) {
		List result= new ArrayList();
		List elements= selection.toList();
		for (Iterator iter= elements.iterator(); iter.hasNext();) {
			Object element= iter.next();
			if (!(element instanceof IWorkingSet))
				return null;
			List projects= getProjects((IWorkingSet)element);
			if (projects == null)
				return null;
			result.addAll(projects);
		}
		return result;
	}

	private List getProjects(IWorkingSet set) {
		List result= new ArrayList();
		IAdaptable[] elements= set.getElements();
		for (int i= 0; i < elements.length; i++) {
			Object element= elements[i];
			IProject project= null;
			if (element instanceof IProject) {
				project= (IProject)element;
			} else if (element instanceof IJavaProject) {
				project= ((IJavaProject)element).getProject();
			}
			if (project == null || !validate(project))
				return null;
			result.add(project);
		}
		return result;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void resourceChanged(IResourceChangeEvent event) {
		IResourceDelta delta = event.getDelta();
		if (delta != null) {
			IResourceDelta[] projDeltas = delta.getAffectedChildren(IResourceDelta.CHANGED);
			for (int i = 0; i < projDeltas.length; ++i) {
				IResourceDelta projDelta = projDeltas[i];
				if ((projDelta.getFlags() & IResourceDelta.OPEN) != 0) {
					Shell shell= getShell();
					if (!shell.isDisposed()) {
						shell.getDisplay().asyncExec(new Runnable() {
							public void run() {
								update(getSelection());
							}
						});
					}
					return;
				}
			}
		}
	}
}
