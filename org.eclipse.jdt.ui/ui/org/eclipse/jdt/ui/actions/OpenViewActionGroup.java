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
import org.eclipse.jdt.internal.ui.actions.RetargetActionIDs;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

import org.eclipse.jdt.ui.IContextMenuConstants;

/**
 * Action group that adds the actions that open a new Jdt view part or
 * an external viewer to the context menu and the action bar's navigate 
 * menu.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class OpenViewActionGroup extends ActionGroup {

	private boolean fEditorIsOwner;
	private IWorkbenchSite fSite;

	private OpenSuperImplementationAction fOpenSuperImplementation;
	private OpenExternalJavadocAction fOpenExternalJavadoc;
	private OpenTypeHierarchyAction fOpenTypeHierarchy;
	private PropertyDialogAction fOpenPropertiesDialog;

	/**
	 * Creates a new <code>OpenActionGroup</code>.
	 * 
	 * @param page the page that owns this action group
	 */
	public OpenViewActionGroup(Page page) {
		createSiteActions(page.getSite());
	}
	
	/**
	 * Creates a new <code>OpenActionGroup</code>.
	 * 
	 * @param part the view part that owns this action group
	 */
	public OpenViewActionGroup(IViewPart part) {
		createSiteActions(part.getSite());
	}
	
	/**
	 * Creates a new <code>OpenActionGroup</code>.
	 * <p>
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * </p>
	 */
	public OpenViewActionGroup(JavaEditor part) {
		fEditorIsOwner= true;
		fOpenSuperImplementation= new OpenSuperImplementationAction(part);
		fOpenExternalJavadoc= new OpenExternalJavadocAction(part);
		fOpenTypeHierarchy= new OpenTypeHierarchyAction(part);
		initialize(part.getEditorSite().getSelectionProvider());
	}

	private void createSiteActions(IWorkbenchSite site) {
		fSite= site;
		fOpenSuperImplementation= new OpenSuperImplementationAction(site);
		fOpenExternalJavadoc= new OpenExternalJavadocAction(site);
		fOpenTypeHierarchy= new OpenTypeHierarchyAction(site);
		fOpenPropertiesDialog= new PropertyDialogAction(site.getShell(), site.getSelectionProvider());
		initialize(site.getSelectionProvider());
	}
	
	private void initialize(ISelectionProvider provider) {
		ISelection selection= provider.getSelection();
		fOpenSuperImplementation.update(selection);
		fOpenExternalJavadoc.update(selection);
		fOpenTypeHierarchy.update(selection);
		if (!fEditorIsOwner) {
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection ss= (IStructuredSelection)selection;
				fOpenPropertiesDialog.selectionChanged(ss);
			} else {
				fOpenPropertiesDialog.selectionChanged(selection);
			}
			provider.addSelectionChangedListener(fOpenSuperImplementation);
			provider.addSelectionChangedListener(fOpenExternalJavadoc);
			provider.addSelectionChangedListener(fOpenTypeHierarchy);
			// no need to register the open properties dialog action since it registers itself
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
		appendToGroup(menu, fOpenTypeHierarchy);
		appendToGroup(menu, fOpenSuperImplementation);
		IStructuredSelection selection= getStructuredSelection();
		if (fOpenPropertiesDialog != null && fOpenPropertiesDialog.isEnabled() && selection != null &&fOpenPropertiesDialog.isApplicableForSelection(selection))
			menu.appendToGroup(IContextMenuConstants.GROUP_PROPERTIES, fOpenPropertiesDialog);
	}

	private void setGlobalActionHandlers(IActionBars actionBars) {
		actionBars.setGlobalActionHandler(RetargetActionIDs.OPEN_SUPER_IMPLEMENTATION, fOpenSuperImplementation);
		actionBars.setGlobalActionHandler(RetargetActionIDs.OPEN_EXTERNAL_JAVA_DOC, fOpenExternalJavadoc);
		actionBars.setGlobalActionHandler(RetargetActionIDs.OPEN_TYPE_HIERARCHY, fOpenTypeHierarchy);
		actionBars.setGlobalActionHandler(IWorkbenchActionConstants.PROPERTIES, fOpenPropertiesDialog);		
	}
	
	private void appendToGroup(IMenuManager menu, IAction action) {
		if (action.isEnabled())
			menu.appendToGroup(IContextMenuConstants.GROUP_OPEN, action);
	}
	
	private IStructuredSelection getStructuredSelection() {
		ISelection selection= getContext().getSelection();
		if (selection instanceof IStructuredSelection)
			return (IStructuredSelection)selection;
		return null;
	}
}
