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

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.core.resources.IncrementalProjectBuilder;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.actions.BuildAction;
import org.eclipse.ui.actions.RefreshAction;

import org.eclipse.jdt.internal.ui.actions.ActionMessages;

import org.eclipse.jdt.ui.IContextMenuConstants;

/**
 * Contributes all build related actions to the context menu and installs action handlers
 * for them in the global menu bar.
 * 
 * @since 2.0
 */
public class BuildActionGroup extends ActionGroup{
	
	private BuildAction fBuildAction;
	private BuildAction fFullBuildAction;
 	private RefreshAction fRefreshAction;

	/**
	 * Creates a new <code>BuildActionGroup</code>.
	 * 
	 * @param part the view part that owns this action group
	 */
	public BuildActionGroup(IViewPart part) {
		Shell shell= part.getSite().getShell();
		ISelectionProvider provider= part.getSite().getSelectionProvider();
		
		fBuildAction= new BuildAction(shell,
				IncrementalProjectBuilder.INCREMENTAL_BUILD);
		fBuildAction.setText(ActionMessages.getString("BuildAction.label")); //$NON-NLS-1$
		
		fFullBuildAction= new BuildAction(shell,
			IncrementalProjectBuilder.FULL_BUILD);
		fFullBuildAction.setText(ActionMessages.getString("RebuildAction.label")); //$NON-NLS-1$
		
		fRefreshAction= new RefreshAction(shell);
		
		provider.addSelectionChangedListener(fBuildAction);
		provider.addSelectionChangedListener(fFullBuildAction);
		provider.addSelectionChangedListener(fRefreshAction);
	}
	
	/**
	 * Returns the refresh action managed by this group.
	 * 
	 * @return the refresh action. If this group doesn't manage a refresh action
	 * 	<code>null</code> is returned.
	 */
	public RefreshAction getRefreshAction() {
		return fRefreshAction;
	}

	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void fillActionBars(IActionBars actionBar) {
		super.fillActionBars(actionBar);
		setGlobalActionHandlers(actionBar);
	}
	
	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void fillContextMenu(IMenuManager menu) {
		super.fillContextMenu(menu);
		appendToGroup(menu, fFullBuildAction);
		appendToGroup(menu, fRefreshAction);
	}
	
	private void setGlobalActionHandlers(IActionBars actionBar) {
		actionBar.setGlobalActionHandler(IWorkbenchActionConstants.BUILD_PROJECT, fBuildAction);
		actionBar.setGlobalActionHandler(IWorkbenchActionConstants.REBUILD_PROJECT, fFullBuildAction);
		actionBar.setGlobalActionHandler(IWorkbenchActionConstants.REFRESH, fRefreshAction);
	}
	
	private void appendToGroup(IMenuManager menu, IAction action) {
		if (action.isEnabled())
			menu.appendToGroup(IContextMenuConstants.GROUP_BUILD, action);
	}			
}
