/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
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
import org.eclipse.jface.viewers.IInputSelectionProvider;
import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.part.Page;

import org.eclipse.jdt.internal.ui.actions.GroupContext;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.search.FindDeclarationsAction;
import org.eclipse.jdt.internal.ui.search.FindDeclarationsInHierarchyAction;
import org.eclipse.jdt.internal.ui.search.FindDeclarationsInWorkingSetAction;
import org.eclipse.jdt.internal.ui.search.FindImplementorsAction;
import org.eclipse.jdt.internal.ui.search.FindImplementorsInWorkingSetAction;
import org.eclipse.jdt.internal.ui.search.FindReadReferencesAction;
import org.eclipse.jdt.internal.ui.search.FindReadReferencesInHierarchyAction;
import org.eclipse.jdt.internal.ui.search.FindReadReferencesInWorkingSetAction;
import org.eclipse.jdt.internal.ui.search.FindReferencesAction;
import org.eclipse.jdt.internal.ui.search.FindReferencesInHierarchyAction;
import org.eclipse.jdt.internal.ui.search.FindReferencesInWorkingSetAction;
import org.eclipse.jdt.internal.ui.search.FindWriteReferencesAction;
import org.eclipse.jdt.internal.ui.search.FindWriteReferencesInHierarchyAction;
import org.eclipse.jdt.internal.ui.search.FindWriteReferencesInWorkingSetAction;
import org.eclipse.jdt.internal.ui.search.JavaSearchGroup;

/**
 * Action group that adds the Java search actions to a context menu and
 * the global menu bar.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class JavaSearchActionGroup extends ActionGroup {

	private JavaSearchGroup fOldGroup;
	private GroupContext fOldContext;

	/**
	 * Creates a new <code>JavaSearchActionGroup</code>.
	 * 
	 * @param part the view part that owns this action group
	 * <p>
	 * Note: this constructor will go away. The final constructor will only take
	 *  an <code>IViewPart</code>
	 * </p>
	 */
	public JavaSearchActionGroup(IViewPart part, IInputSelectionProvider provider) {
		fOldGroup= new JavaSearchGroup();
		fOldContext= new GroupContext(provider);
	}
	
	/**
	 * Creates a new <code>JavaSearchActionGroup</code>.
	 * 
	 * @param part the view part that owns this action group
	 * <p>
	 * Note: this constructor will go away. The final constructor will only take
	 *  an <code>IViewPart</code>
	 * </p>
	 */
	public JavaSearchActionGroup(IViewPart part, ISelectionProvider provider) {
		fOldGroup= new JavaSearchGroup();
		fOldContext= new GroupContext(provider);
	}
	
	/**
	 * Creates a new <code>JavaSearchActionGroup</code>.
	 * 
	 * @param page the page that owns this action group
	 * <p>
	 * Note: this constructor will go away. The final constructor will only take
	 *  an <code>Page</code>
	 * </p>
	 */
	public JavaSearchActionGroup(Page page, IInputSelectionProvider provider) {
		fOldGroup= new JavaSearchGroup();
		fOldContext= new GroupContext(provider);
	}

	/**
	 * Creates a new Java search action group.
	 * <p>
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * </p>
	 */
	public JavaSearchActionGroup(JavaEditor editor) {
//		fEditor= editor;
		fOldGroup= new JavaSearchGroup(false);
	}
	
	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void fillActionBars(IActionBars actionBar) {
		super.fillActionBars(actionBar);
		setGlobalActionHandlers(actionBar);
	}
	
	private void setGlobalActionHandlers(IActionBars actionBar) {
		actionBar.setGlobalActionHandler("org.eclipse.jdt.ui.actions.ReferencesInWorkspace", new FindReferencesAction()); //$NON-NLS-1$
		actionBar.setGlobalActionHandler("org.eclipse.jdt.ui.actions.ReferencesInHierarchy", new FindReferencesInHierarchyAction()); //$NON-NLS-1$
		actionBar.setGlobalActionHandler("org.eclipse.jdt.ui.actions.ReferencesInWorkingSet", new FindReferencesInWorkingSetAction()); //$NON-NLS-1$
		
		actionBar.setGlobalActionHandler("org.eclipse.jdt.ui.actions.ReadAccessInWorkspace", new FindReadReferencesAction()); //$NON-NLS-1$
		actionBar.setGlobalActionHandler("org.eclipse.jdt.ui.actions.ReadAccessInHierarchy", new FindReadReferencesInHierarchyAction()); //$NON-NLS-1$
		actionBar.setGlobalActionHandler("org.eclipse.jdt.ui.actions.ReadAccessInWorkingSet", new FindReadReferencesInWorkingSetAction()); //$NON-NLS-1$

		actionBar.setGlobalActionHandler("org.eclipse.jdt.ui.actions.WriteAccessInWorkspace", new FindWriteReferencesAction()); //$NON-NLS-1$
		actionBar.setGlobalActionHandler("org.eclipse.jdt.ui.actions.WriteAccessInHierarchy", new FindWriteReferencesInHierarchyAction()); //$NON-NLS-1$
		actionBar.setGlobalActionHandler("org.eclipse.jdt.ui.actions.WriteAccessInWorkingSet", new FindWriteReferencesInWorkingSetAction()); //$NON-NLS-1$

		actionBar.setGlobalActionHandler("org.eclipse.jdt.ui.actions.DeclarationsInWorkspace", new FindDeclarationsAction()); //$NON-NLS-1$
		actionBar.setGlobalActionHandler("org.eclipse.jdt.ui.actions.DeclarationsInWorkingSet", new FindDeclarationsInWorkingSetAction()); //$NON-NLS-1$
		actionBar.setGlobalActionHandler("org.eclipse.jdt.ui.actions.DeclarationsInHierarchy", new FindDeclarationsInHierarchyAction()); //$NON-NLS-1$

		actionBar.setGlobalActionHandler("org.eclipse.jdt.ui.actions.ImplementorsInWorkspace", new FindImplementorsAction()); //$NON-NLS-1$
		actionBar.setGlobalActionHandler("org.eclipse.jdt.ui.actions.ImplementorsInWorkingSet", new FindImplementorsInWorkingSetAction()); //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void fillContextMenu(IMenuManager menu) {
		super.fillContextMenu(menu);
		fOldGroup.fill(menu, fOldContext);
	}	
}
