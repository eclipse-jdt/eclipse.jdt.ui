/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jdt.internal.ui.dialogs.ListDialog;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

/**
 * Action to open a closed project. Action presents a dialog from which the
 * user can select the projects to be opened.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 * @deprecated Use <tt>org.eclipse.ui.actions.OpenResourceAction</tt> instead;
 * the package explorer now displays closed projects as well.
 */
public class OpenProjectAction extends Action implements IResourceChangeListener {
	
	private IWorkbenchSite fSite;

	/**
	 * Creates a new <code>OpenProjectAction</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public OpenProjectAction(IWorkbenchSite site) {
		Assert.isNotNull(site);
		fSite= site;
		setEnabled(hasCloseProjects());
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.OPEN_PROJECT_ACTION);
	}
	
	/*
	 * @see IResourceChangeListener#resourceChanged(IResourceChangeEvent)
	 */
	public void resourceChanged(IResourceChangeEvent event) {
		IResourceDelta delta = event.getDelta();
		if (delta != null) {
			IResourceDelta[] projDeltas = delta.getAffectedChildren(IResourceDelta.CHANGED);
			for (int i = 0; i < projDeltas.length; ++i) {
				IResourceDelta projDelta = projDeltas[i];
				if ((projDelta.getFlags() & IResourceDelta.OPEN) != 0) {
					setEnabled(hasCloseProjects());
					return;
				}
			}
		}
	}
		
	/*
	 * @see IAction#run()
	 */
	public void run() {
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(fSite.getShell(), new JavaElementLabelProvider());
		dialog.setTitle(ActionMessages.getString("OpenProjectAction.dialog.title")); //$NON-NLS-1$
		dialog.setMessage(ActionMessages.getString("OpenProjectAction.dialog.message")); //$NON-NLS-1$
		dialog.setElements(getClosedProjects());
		dialog.setMultipleSelection(true);
		int result= dialog.open();
		if (result != Dialog.OK)
			return;
		final Object[] projects= dialog.getResult();
		final List nonJavaProjects= new ArrayList(3);
		IWorkspaceRunnable runnable= createRunnable(projects, nonJavaProjects);
		ProgressMonitorDialog pd= new ProgressMonitorDialog(fSite.getShell());
		try {
			pd.run(true, true, new WorkbenchRunnableAdapter(runnable));
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, fSite.getShell(), 
				ActionMessages.getString("OpenProjectAction.dialog.title"), //$NON-NLS-1$
				ActionMessages.getString("OpenProjectAction.error.message")); //$NON-NLS-1$
		} catch (InterruptedException e) {
		}
		showWarningDialog(nonJavaProjects);
	}
	
	private IWorkspaceRunnable createRunnable(final Object[] projects, final List nonJavaProjects) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				monitor.beginTask("", projects.length); //$NON-NLS-1$
				MultiStatus errorStatus= null;
				for (int i = 0; i < projects.length; i++) {
					IProject project= (IProject)projects[i];
					try {
						project.open(new SubProgressMonitor(monitor, 1));
						if (project.getNature(JavaCore.NATURE_ID) == null) {
							nonJavaProjects.add(project);
						}		
					} catch (CoreException e) {
						if (errorStatus == null)
							errorStatus = new MultiStatus(JavaPlugin.getPluginId(), IStatus.ERROR, ActionMessages.getString("OpenProjectAction.error.message"), e); //$NON-NLS-1$
						errorStatus.merge(e.getStatus());
					}
				}
				monitor.done();
				if (errorStatus != null)
					throw new CoreException(errorStatus);
			}
		};
	}
	
	private void showWarningDialog(final List nonJavaProjects) {
		int size= nonJavaProjects.size();
		if (size > 0) {
			ListDialog warningDialog= new ListDialog(fSite.getShell());
			warningDialog.setAddCancelButton(false);
			warningDialog.setTitle(ActionMessages.getString("OpenProjectAction.dialog.title")); //$NON-NLS-1$
			if (size == 1)
				warningDialog.setMessage(ActionMessages.getString("OpenProjectAction.no_java_nature.one")); //$NON-NLS-1$
			else
				warningDialog.setMessage(ActionMessages.getString("OpenProjectAction.no_java_nature.multiple")); //$NON-NLS-1$
			warningDialog.setContentProvider(new IStructuredContentProvider() {
				public Object[] getElements(Object inputElement) {
					return ((List)inputElement).toArray();
				}
				public void dispose() {
				}
				public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				}
			});
			warningDialog.setLabelProvider(new JavaElementLabelProvider());
			warningDialog.setInput(nonJavaProjects);
			warningDialog.open();
		}
	}
	
	private Object[] getClosedProjects() {
		IProject[] projects= ResourcesPlugin.getWorkspace().getRoot().getProjects();
		List result= new ArrayList(5);
		for (int i = 0; i < projects.length; i++) {
			IProject project= projects[i];
			if (!project.isOpen())
				result.add(project);
		}
		return result.toArray();
	}
	
	private boolean hasCloseProjects() {
		IProject[] projects= ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (int i = 0; i < projects.length; i++) {
			if (!projects[i].isOpen())
				return true;
		}
		return false;
	}
}
