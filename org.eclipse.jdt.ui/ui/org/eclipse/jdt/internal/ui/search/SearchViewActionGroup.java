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
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.actions.ActionGroup;

import org.eclipse.jdt.ui.IContextMenuConstants;

import org.eclipse.jdt.ui.actions.JavaSearchActionGroup;
import org.eclipse.jdt.ui.actions.JdtActionConstants;
import org.eclipse.jdt.ui.actions.OpenAction;
import org.eclipse.jdt.ui.actions.OpenTypeHierarchyAction;

import org.eclipse.jdt.internal.ui.actions.CompositeActionGroup;

class SearchViewActionGroup extends CompositeActionGroup {

	private ISelectionProvider fSelectionProvider;
	private IActionBars fActionBars;

	private OpenTypeHierarchyAction fOpenTypeHierarchyAction;
	private OpenAction fOpenAction;
  	
	public SearchViewActionGroup(IViewPart part) {
		Assert.isNotNull(part);
		fSelectionProvider= part.getSite().getSelectionProvider();
		
		setGroups(new ActionGroup[] { new JavaSearchActionGroup(part)});
		initializeActions(new SearchViewSiteAdapter(part.getSite()));
	}

	private void initializeActions(IWorkbenchSite site) {
		ISelectionProvider provider= site.getSelectionProvider();
		IStructuredSelection selection= (IStructuredSelection)provider.getSelection();

		// Open Type Hierarchy (F3)
		fOpenAction= new OpenAction(site);
		fOpenAction.update(selection);
		provider.addSelectionChangedListener(fOpenAction);
		
		// Open Type Hierarchy (F4)
		fOpenTypeHierarchyAction= new OpenTypeHierarchyAction(site);
		fOpenTypeHierarchyAction.update(selection);
		provider.addSelectionChangedListener(fOpenTypeHierarchyAction);
	}

	public void dispose() {
		if (fSelectionProvider != null) {
			fSelectionProvider.removeSelectionChangedListener(fOpenTypeHierarchyAction);
			fSelectionProvider.removeSelectionChangedListener(fOpenAction);
		}
		fOpenAction= null;
		fOpenTypeHierarchyAction= null;
		updateGlobalActionHandlers();
		super.dispose();
	}
	
	public void fillActionBars(IActionBars actionBars) {
		Assert.isNotNull(actionBars);
		super.fillActionBars(actionBars);
		fActionBars= actionBars;
		updateGlobalActionHandlers();
	}

	private void updateGlobalActionHandlers() {
		if (fActionBars != null) {
			fActionBars.setGlobalActionHandler(JdtActionConstants.OPEN_TYPE_HIERARCHY, fOpenTypeHierarchyAction);
			fActionBars.setGlobalActionHandler(JdtActionConstants.OPEN, fOpenAction);
		}
	}

	//---- Context menu -------------------------------------------------------------------------

	public void fillContextMenu(IMenuManager menu) {		
		super.fillContextMenu(menu);
		appendToGroup(menu, fOpenAction, IContextMenuConstants.GROUP_OPEN);
		appendToGroup(menu, fOpenTypeHierarchyAction, IContextMenuConstants.GROUP_OPEN);		
	}
	
	private void appendToGroup(IMenuManager menu, IAction action, String groupName) {
		if (action.isEnabled())
			menu.appendToGroup(groupName, action);
	}
}

