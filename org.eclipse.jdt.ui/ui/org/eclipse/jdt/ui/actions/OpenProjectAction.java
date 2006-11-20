/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.OpenResourceAction;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

/**
 * Action to open a closed project. Action either opens the closed projects
 * provided by the structured selection or presents a dialog from which the
 * user can select the projects to be opened.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class OpenProjectAction extends SelectionDispatchAction implements IResourceChangeListener {
	
	private OpenResourceAction fWorkbenchAction;

	/**
	 * Creates a new <code>OpenProjectAction</code>. The action requires
	 * that the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public OpenProjectAction(IWorkbenchSite site) {
		super(site);
		fWorkbenchAction= new OpenResourceAction(site.getShell());
		setText(fWorkbenchAction.getText());
		setToolTipText(fWorkbenchAction.getToolTipText());
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.OPEN_PROJECT_ACTION);
		setEnabled(hasCloseProjects());
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
	
	//---- normal selection -------------------------------------
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#selectionChanged(org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(ISelection selection) {
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#run(org.eclipse.jface.viewers.ISelection)
	 */
	public void run(ISelection selection) {
		internalRun(null);
	}
	
	//---- structured selection ---------------------------------------
		
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#run(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void run(IStructuredSelection selection) {
		Object[] array= selection.toArray();
		ArrayList allClosedProjects= new ArrayList();
		boolean otherElementsSelected= false;
		for (int i= 0; i < array.length; i++) {
			Object curr= array[i];
			if (curr instanceof IAdaptable) {
				IProject project= (IProject) ((IAdaptable) curr).getAdapter(IProject.class);
				if (project != null && !project.isOpen()) {
					allClosedProjects.add(project);
				} else {
					otherElementsSelected= true;
				}
			}
		}
		if (!otherElementsSelected && !allClosedProjects.isEmpty()) {
			fWorkbenchAction.selectionChanged(new StructuredSelection(allClosedProjects));
			fWorkbenchAction.run();
		} else {
			internalRun(allClosedProjects);
		}
	}
	
	private void internalRun(List initialSelection) {
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(getShell(), new JavaElementLabelProvider());
		dialog.setTitle(ActionMessages.OpenProjectAction_dialog_title); 
		dialog.setMessage(ActionMessages.OpenProjectAction_dialog_message); 
		dialog.setElements(getClosedProjects());
		dialog.setMultipleSelection(true);
		if (initialSelection != null && !initialSelection.isEmpty()) {
			dialog.setInitialElementSelections(initialSelection);
		}
		int result= dialog.open();
		if (result != Window.OK)
			return;
		final Object[] projects= dialog.getResult();
		IWorkspaceRunnable runnable= createRunnable(projects);
		try {
			PlatformUI.getWorkbench().getProgressService().run(true, true, new WorkbenchRunnableAdapter(runnable));
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, getShell(), 
				ActionMessages.OpenProjectAction_dialog_title, 
				ActionMessages.OpenProjectAction_error_message); 
		} catch (InterruptedException e) {
		}
	}
	
	private IWorkspaceRunnable createRunnable(final Object[] projects) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				monitor.beginTask("", projects.length); //$NON-NLS-1$
				MultiStatus errorStatus= null;
				for (int i = 0; i < projects.length; i++) {
					IProject project= (IProject)projects[i];
					try {
						project.open(new SubProgressMonitor(monitor, 1));
					} catch (CoreException e) {
						if (errorStatus == null)
							errorStatus = new MultiStatus(JavaPlugin.getPluginId(), IStatus.ERROR, ActionMessages.OpenProjectAction_error_message, e); 
						errorStatus.merge(e.getStatus());
					}
				}
				monitor.done();
				if (errorStatus != null)
					throw new CoreException(errorStatus);
			}
		};
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
