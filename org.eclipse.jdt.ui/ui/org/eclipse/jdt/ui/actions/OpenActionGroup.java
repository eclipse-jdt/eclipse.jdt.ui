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

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.part.Page;

import org.eclipse.jdt.internal.ui.actions.RetargetActionIDs;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

/**
 * Action group that adds the open actions to a context menu and
 * the action bar's navigate menu.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class OpenActionGroup extends ActionGroup {

	private OpenAction fOpen;
	private OpenSuperImplementationAction fOpenSuperImplementation;
	private OpenExternalJavadocAction fOpenExternalJavadocAction;
	private OpenTypeHierarchyAction fOpenTypeHierarchyAction;

	/**
	 * Creates a new <code>OpenActionGroup</code>.
	 * 
	 * @param page the page that owns this action group
	 */
	public OpenActionGroup(Page page) {
		this(UnifiedSite.create(page.getSite()));
	}
	
	/**
	 * Creates a new <code>OpenActionGroup</code>.
	 * 
	 * @param part the view part that owns this action group
	 */
	public OpenActionGroup(IViewPart part) {
		this(UnifiedSite.create(part.getSite()));
	}
	
	/**
	 * Creates a new <code>OpenActionGroup</code>.
	 * <p>
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * </p>
	 */
	public OpenActionGroup(JavaEditor part) {
		fOpen= new OpenAction(part);
		fOpenSuperImplementation= new OpenSuperImplementationAction(part);
		fOpenExternalJavadocAction= new OpenExternalJavadocAction(part);
		fOpenTypeHierarchyAction= new OpenTypeHierarchyAction(part);
		initialize(part.getEditorSite().getSelectionProvider(), true);
	}

	private OpenActionGroup(UnifiedSite site) {
		fOpen= new OpenAction(site);
		fOpenSuperImplementation= new OpenSuperImplementationAction(site);
		fOpenExternalJavadocAction= new OpenExternalJavadocAction(site);
		fOpenTypeHierarchyAction= new OpenTypeHierarchyAction(site);
		initialize(site.getSelectionProvider(), false);
	}
	
	private void initialize(ISelectionProvider provider, boolean isJavaEditor) {
		ISelection selection= provider.getSelection();
		fOpen.update(selection);
		fOpenSuperImplementation.update(selection);
		fOpenExternalJavadocAction.update(selection);
		fOpenTypeHierarchyAction.update(selection);
		if (!isJavaEditor) {
			provider.addSelectionChangedListener(fOpen);
			provider.addSelectionChangedListener(fOpenSuperImplementation);
			provider.addSelectionChangedListener(fOpenExternalJavadocAction);
			provider.addSelectionChangedListener(fOpenTypeHierarchyAction);
		}
	}

	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void fillActionBars(IActionBars actionBar) {
		super.fillActionBars(actionBar);
		actionBar.setGlobalActionHandler(RetargetActionIDs.OPEN, fOpen);
		actionBar.setGlobalActionHandler(RetargetActionIDs.OPEN_SUPER_IMPLEMENTATION, fOpenSuperImplementation);
		actionBar.setGlobalActionHandler(RetargetActionIDs.OPEN_EXTERNAL_JAVA_DOC, fOpenExternalJavadocAction);
		actionBar.setGlobalActionHandler(RetargetActionIDs.OPEN_TYPE_HIERARCHY, fOpenTypeHierarchyAction);
	}
	
	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void fillContextMenu(IMenuManager menu) {
		super.fillContextMenu(menu);
	}
}
