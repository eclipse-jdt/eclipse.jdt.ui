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
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.actions.OpenWithMenu;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.eclipse.ui.part.Page;

import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

import org.eclipse.jdt.ui.IContextMenuConstants;

/**
 * Action group that adds the actions that open a new editor to the 
 * context menu and the action bar's navigate menu.
 *
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class OpenEditorActionGroup extends ActionGroup {

	private IWorkbenchSite fSite;
	private boolean fIsEditorOwner;
	private OpenAction fOpen;

	/**
	 * Creates a new <code>OpenActionGroup</code>.
	 * 
	 * @param part the view part that owns this action group
	 */
	public OpenEditorActionGroup(IViewPart part) {
		fSite= part.getSite();
		fOpen= new OpenAction(fSite);
		initialize(fSite.getSelectionProvider());
	}
	
	/**
	 * Creates a new <code>OpenActionGroup</code>.
	 * <p>
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * </p>
	 */
	public OpenEditorActionGroup(JavaEditor part) {
		fIsEditorOwner= true;
		fOpen= new OpenAction(part);
		fSite= part.getEditorSite();
		initialize(fSite.getSelectionProvider());
	}

	/**
	 * Returns the <code>OpenAction</code> managed by this action
	 * group. 
	 * 
	 * @return the open action. Returns <code>null</code> if the group
	 * 	doesn't provide any open action
	 */
	public OpenAction getOpenAction() {
		return fOpen;
	}

	private void initialize(ISelectionProvider provider) {
		ISelection selection= provider.getSelection();
		fOpen.update(selection);
		if (!fIsEditorOwner) {
			provider.addSelectionChangedListener(fOpen);
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
		if (!fIsEditorOwner) {
			appendToGroup(menu, fOpen);
			addOpenWithMenu(menu);
		}
	}

	private void setGlobalActionHandlers(IActionBars actionBars) {
		actionBars.setGlobalActionHandler(JdtActionConstants.OPEN, fOpen);
	}
	
	private void appendToGroup(IMenuManager menu, IAction action) {
		if (action.isEnabled())
			menu.appendToGroup(IContextMenuConstants.GROUP_OPEN, action);
	}
	
	private void addOpenWithMenu(IMenuManager menu) {
		ISelection selection= getContext().getSelection();
		if (selection.isEmpty() || !(selection instanceof IStructuredSelection))
			return;
		IStructuredSelection ss= (IStructuredSelection)selection;
		if (ss.size() != 1)
			return;

		Object o= ss.getFirstElement();
		IAdaptable element= null;
		if (o instanceof IType) {
			element= ((IType)o).getCompilationUnit();
		} else if (o instanceof IAdaptable) {
			element= (IAdaptable)o;
		}
		if (element == null)
			return;
		Object resource= element.getAdapter(IResource.class);
		if (!(resource instanceof IFile))
			return; 

		// Create a menu flyout.
		IMenuManager submenu= new MenuManager(ActionMessages.getString("OpenWithMenu.label")); //$NON-NLS-1$
		submenu.add(new OpenWithMenu(fSite.getPage(), (IFile) resource));

		// Add the submenu.
		menu.appendToGroup(IContextMenuConstants.GROUP_OPEN, submenu);
	}
}
