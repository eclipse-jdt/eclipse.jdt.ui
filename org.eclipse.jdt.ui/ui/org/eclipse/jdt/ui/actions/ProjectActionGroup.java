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

import java.util.Iterator;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.actions.CloseResourceAction;
import org.eclipse.ui.actions.CloseUnrelatedProjectsAction;

import org.eclipse.ui.ide.IDEActionFactory;

import org.eclipse.jdt.ui.IContextMenuConstants;

/**
 * Adds actions to open and close a project to the global menu bar.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class ProjectActionGroup extends ActionGroup {

	private IWorkbenchSite fSite;

	private OpenProjectAction fOpenAction;
	private CloseResourceAction fCloseAction;
	private CloseResourceAction fCloseUnrelatedAction;

	/**
	 * Creates a new <code>ProjectActionGroup</code>. The group requires
	 * that the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param part the view part that owns this action group
	 */
	public ProjectActionGroup(IViewPart part) {
		fSite = part.getSite();
		Shell shell= fSite.getShell();
		ISelectionProvider provider= fSite.getSelectionProvider();
		ISelection selection= provider.getSelection();
		
		fCloseAction= new CloseResourceAction(shell);
		fCloseAction.setActionDefinitionId("org.eclipse.ui.project.closeProject"); //$NON-NLS-1$
		
		fCloseUnrelatedAction= new CloseUnrelatedProjectsAction(shell);
		fCloseUnrelatedAction.setActionDefinitionId("org.eclipse.ui.project.closeUnrelatedProjects"); //$NON-NLS-1$
		
		fOpenAction= new OpenProjectAction(fSite);
		fOpenAction.setActionDefinitionId("org.eclipse.ui.project.openProject"); //$NON-NLS-1$
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection s= (IStructuredSelection)selection;
			fOpenAction.selectionChanged(s);
			fCloseAction.selectionChanged(s);
			fCloseUnrelatedAction.selectionChanged(s);
		}
		provider.addSelectionChangedListener(fOpenAction);
		provider.addSelectionChangedListener(fCloseAction);
		provider.addSelectionChangedListener(fCloseUnrelatedAction);
		IWorkspace workspace= ResourcesPlugin.getWorkspace();
		workspace.addResourceChangeListener(fOpenAction);
		workspace.addResourceChangeListener(fCloseAction);
		workspace.addResourceChangeListener(fCloseUnrelatedAction);
	}
	
	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		actionBars.setGlobalActionHandler(IDEActionFactory.CLOSE_PROJECT.getId(), fCloseAction);
		actionBars.setGlobalActionHandler(IDEActionFactory.CLOSE_UNRELATED_PROJECTS.getId(), fCloseUnrelatedAction);
		actionBars.setGlobalActionHandler(IDEActionFactory.OPEN_PROJECT.getId(), fOpenAction);
	}
	
	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void fillContextMenu(IMenuManager menu) {
		super.fillContextMenu(menu);
		if (fOpenAction.isEnabled() && areOnlyClosedProjectsSelected(fOpenAction.getSelection()))
			menu.appendToGroup(IContextMenuConstants.GROUP_BUILD, fOpenAction);
		if (fCloseAction.isEnabled())
			menu.appendToGroup(IContextMenuConstants.GROUP_BUILD, fCloseAction);
		if (fCloseUnrelatedAction.isEnabled() && areOnlyProjectsSelected(fCloseUnrelatedAction.getStructuredSelection()))
			menu.appendToGroup(IContextMenuConstants.GROUP_BUILD, fCloseUnrelatedAction);
	}

	private boolean areOnlyProjectsSelected(IStructuredSelection selection) {
		if (selection.isEmpty())
			return false;
		
		Iterator iter= selection.iterator();
		while (iter.hasNext()) {
			Object obj= iter.next();
			if (obj instanceof IAdaptable) {
				if (((IAdaptable)obj).getAdapter(IProject.class) == null)
					return false;
			}
		}
		return true;
	}
	
	private boolean areOnlyClosedProjectsSelected(ISelection selection) {
		if (selection.isEmpty() || !(selection instanceof IStructuredSelection))
			return false;
		
		Iterator iter= ((IStructuredSelection) selection).iterator();
		while (iter.hasNext()) {
			Object obj= iter.next();
			if (obj instanceof IAdaptable) {
				IProject project= (IProject) ((IAdaptable)obj).getAdapter(IProject.class);
				if (project == null || project.isOpen())
					return false;
			}
		}
		return true;
	}

	/*
	 * @see ActionGroup#dispose()
	 */
	public void dispose() {
		ISelectionProvider provider= fSite.getSelectionProvider();
		provider.removeSelectionChangedListener(fOpenAction);
		provider.removeSelectionChangedListener(fCloseAction);
		provider.removeSelectionChangedListener(fCloseUnrelatedAction);
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		workspace.removeResourceChangeListener(fOpenAction);
		workspace.removeResourceChangeListener(fCloseAction);
		workspace.removeResourceChangeListener(fCloseUnrelatedAction);
		super.dispose();
	}
}
