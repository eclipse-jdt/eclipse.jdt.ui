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

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.actions.CloseResourceAction;

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
		
		fOpenAction= new OpenProjectAction(fSite);
		fCloseAction= new CloseResourceAction(shell);
		if (selection instanceof IStructuredSelection)
			fCloseAction.selectionChanged((IStructuredSelection)selection);
		provider.addSelectionChangedListener(fCloseAction);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(fOpenAction);
	}
	
	/*
	 * @see ActionGroup#fillActionBars(IActionBars)
	 */
	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		actionBars.setGlobalActionHandler(IWorkbenchActionConstants.CLOSE_PROJECT, fCloseAction);
		actionBars.setGlobalActionHandler(IWorkbenchActionConstants.OPEN_PROJECT, fOpenAction);
	}
	
	/*
	 * @see ActionGroup#dispose()
	 */
	public void dispose() {
		fSite.getSelectionProvider().removeSelectionChangedListener(fCloseAction);
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(fOpenAction);
		super.dispose();
	}
}
