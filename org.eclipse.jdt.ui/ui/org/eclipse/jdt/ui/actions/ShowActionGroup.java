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

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.part.Page;

import org.eclipse.jdt.ui.IContextMenuConstants;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;

/**
 * Action group that adds the show actions to a context menu and
 * the action bar's navigate menu.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class ShowActionGroup extends ActionGroup {

	private boolean fIsPackageExplorer;

	private IWorkbenchSite fSite;
	private ShowInPackageViewAction fShowInPackagesViewAction;
	private ShowInNavigatorViewAction fShowInNavigatorViewAction;

	/**
	 * Creates a new <code>ShowActionGroup</code>. The action requires 
	 * that the selection provided by the page's selection provider is of type 
	 * <code>org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param page the page that owns this action group
	 */
	public ShowActionGroup(Page page) {
		this(page.getSite());
	}
	
	/**
	 * Creates a new <code>ShowActionGroup</code>. The action requires 
	 * that the selection provided by the part's selection provider is of type 
	 * <code>org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param part the view part that owns this action group
	 */
	public ShowActionGroup(IViewPart part) {
		this(part.getSite());
		fIsPackageExplorer= part instanceof PackageExplorerPart;
	}
	
	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public ShowActionGroup(JavaEditor part) {
		fShowInPackagesViewAction= new ShowInPackageViewAction(part);
		fShowInPackagesViewAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SHOW_IN_PACKAGE_VIEW);
		part.setAction("ShowInPackageView", fShowInPackagesViewAction); //$NON-NLS-1$

		fShowInNavigatorViewAction= new ShowInNavigatorViewAction(part);
		fShowInNavigatorViewAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SHOW_IN_NAVIGATOR_VIEW);
		part.setAction("ShowInNavigatorView", fShowInNavigatorViewAction); //$NON-NLS-1$
		initialize(part.getSite(), true);
	}

	private ShowActionGroup(IWorkbenchSite site) {
		fShowInPackagesViewAction= new ShowInPackageViewAction(site);
		fShowInNavigatorViewAction= new ShowInNavigatorViewAction(site);
		initialize(site , false);		
	}

	private void initialize(IWorkbenchSite site, boolean isJavaEditor) {
		fSite= site;
		ISelectionProvider provider= fSite.getSelectionProvider();
		ISelection selection= provider.getSelection();
		fShowInPackagesViewAction.update(selection);
		fShowInNavigatorViewAction.update(selection);
		if (!isJavaEditor) {
			provider.addSelectionChangedListener(fShowInPackagesViewAction);
			provider.addSelectionChangedListener(fShowInNavigatorViewAction);
		}
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
		if (!fIsPackageExplorer) {
			appendToGroup(menu, fShowInPackagesViewAction);
		}
	}
	
	/*
	 * @see ActionGroup#dispose()
	 */
	public void dispose() {
		ISelectionProvider provider= fSite.getSelectionProvider();
		provider.removeSelectionChangedListener(fShowInPackagesViewAction);
		provider.removeSelectionChangedListener(fShowInNavigatorViewAction);
		super.dispose();
	}
	
	private void setGlobalActionHandlers(IActionBars actionBar) {
		if (!fIsPackageExplorer)
			actionBar.setGlobalActionHandler(JdtActionConstants.SHOW_IN_PACKAGE_VIEW, fShowInPackagesViewAction);
		actionBar.setGlobalActionHandler(JdtActionConstants.SHOW_IN_NAVIGATOR_VIEW, fShowInNavigatorViewAction);
	}
	
	private void appendToGroup(IMenuManager menu, IAction action) {
		if (action.isEnabled())
			menu.appendToGroup(IContextMenuConstants.GROUP_SHOW, action);
	}		
}
