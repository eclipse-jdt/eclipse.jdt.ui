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
import org.eclipse.ui.IWorkbenchSite;
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
	private JavaEditor fEditor;
	private IWorkbenchSite fSite;

	private FindReferencesAction fFindReferencesAction;
	private FindReferencesInHierarchyAction fFindReferencesInHierarchyAction;
	private FindReferencesInWorkingSetAction fFindReferencesInWorkingSetAction;
	private FindReadReferencesAction fFindReadReferencesAction;
	private FindReadReferencesInHierarchyAction fFindReadReferencesInHierarchyAction;
	private FindReadReferencesInWorkingSetAction fFindReadReferencesInWorkingSetAction;
	private FindWriteReferencesAction fFindWriteReferencesAction;
	private FindWriteReferencesInHierarchyAction fFindWriteReferencesInHierarchyAction;
	private FindWriteReferencesInWorkingSetAction fFindWriteReferencesInWorkingSetAction;
	private FindDeclarationsAction fFindDeclarationsAction;
	private FindDeclarationsInWorkingSetAction fFindDeclarationsInWorkingSetAction;
	private FindDeclarationsInHierarchyAction fFindDeclarationsInHierarchyAction;
	private FindImplementorsAction fFindImplementorsAction;
	private FindImplementorsInWorkingSetAction fFindImplementorsInWorkingSetAction;
	
	
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
		this(part.getViewSite());
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
		this(part.getViewSite());
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
		this(page.getSite());
		fOldContext= new GroupContext(provider);
	}

	/**
	 * Creates a new Java search action group.
	 * <p>
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * </p>
	 */
	public JavaSearchActionGroup(IWorkbenchSite site) {
		fFindReferencesAction= new FindReferencesAction(site);
		fFindReferencesInHierarchyAction= new FindReferencesInHierarchyAction(site);
		fFindReferencesInWorkingSetAction= new FindReferencesInWorkingSetAction(site);
		
		fFindReadReferencesAction= new FindReadReferencesAction(site);
		fFindReadReferencesInHierarchyAction= new FindReadReferencesInHierarchyAction(site);
		fFindReadReferencesInWorkingSetAction= new FindReadReferencesInWorkingSetAction(site);

		fFindWriteReferencesAction= new FindWriteReferencesAction(site);
		fFindWriteReferencesInHierarchyAction= new FindWriteReferencesInHierarchyAction(site);
		fFindWriteReferencesInWorkingSetAction= new FindWriteReferencesInWorkingSetAction(site);

		fFindDeclarationsAction= new FindDeclarationsAction(site);
		fFindDeclarationsInWorkingSetAction= new FindDeclarationsInWorkingSetAction(site);
		fFindDeclarationsInHierarchyAction= new FindDeclarationsInHierarchyAction(site);

		fFindImplementorsAction= new FindImplementorsAction(site);
		fFindImplementorsInWorkingSetAction= new FindImplementorsInWorkingSetAction(site);
		
		fOldGroup= new JavaSearchGroup(site);
		
		initialize(site, false);
	}

	/**
	 * Creates a new Java search action group.
	 * <p>
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * </p>
	 */
	public JavaSearchActionGroup(JavaEditor editor) {
		fEditor= editor;
		
		fFindReferencesAction= new FindReferencesAction(editor);
		fFindReferencesAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SEARCH_REFERENCES_IN_WORKSPACE);
		editor.setAction("SearchReferencesInWorkspace", fFindReferencesAction); //$NON-NLS-1$

		fFindReferencesInHierarchyAction= new FindReferencesInHierarchyAction(editor);
		fFindReferencesInHierarchyAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SEARCH_REFERENCES_IN_HIERARCHY);
		editor.setAction("SearchReferencesInHierarchy", fFindReferencesInHierarchyAction); //$NON-NLS-1$
		
		fFindReferencesInWorkingSetAction= new FindReferencesInWorkingSetAction(editor);
		fFindReferencesInWorkingSetAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SEARCH_REFERENCES_IN_WORKING_SET);
		editor.setAction("SearchReferencesInWorkingSet", fFindReferencesInWorkingSetAction); //$NON-NLS-1$
		
		fFindReadReferencesAction= new FindReadReferencesAction(editor);
		fFindReadReferencesAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SEARCH_READ_ACCESS_IN_WORKSPACE);
		editor.setAction("SearchReadAccessInWorkspace", fFindReadReferencesAction); //$NON-NLS-1$		
		
		fFindReadReferencesInHierarchyAction= new FindReadReferencesInHierarchyAction(editor);
		fFindReadReferencesInHierarchyAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SEARCH_READ_ACCESS_IN_HIERARCHY);
		editor.setAction("SearchReadAccessInHierarchy", fFindReadReferencesInHierarchyAction); //$NON-NLS-1$		

		fFindReadReferencesInWorkingSetAction= new FindReadReferencesInWorkingSetAction(editor);
		fFindReadReferencesInWorkingSetAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SEARCH_READ_ACCESS_IN_WORKING_SET);
		editor.setAction("SearchReadAccessInWorkingSet", fFindReadReferencesInWorkingSetAction); //$NON-NLS-1$		

		fFindWriteReferencesAction= new FindWriteReferencesAction(editor);
		fFindWriteReferencesAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SEARCH_WRITE_ACCESS_IN_WORKSPACE);
		editor.setAction("SearchWriteAccessInWorkspace", fFindWriteReferencesAction); //$NON-NLS-1$		

		fFindWriteReferencesInHierarchyAction= new FindWriteReferencesInHierarchyAction(editor);
		fFindWriteReferencesInHierarchyAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SEARCH_WRITE_ACCESS_IN_HIERARCHY);
		editor.setAction("SearchWriteAccessInHierarchy", fFindWriteReferencesInHierarchyAction); //$NON-NLS-1$		

		fFindWriteReferencesInWorkingSetAction= new FindWriteReferencesInWorkingSetAction(editor);
		fFindWriteReferencesInWorkingSetAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SEARCH_WRITE_ACCESS_IN_WORKING_SET);
		editor.setAction("SearchWriteAccessInWorkingSet", fFindWriteReferencesInWorkingSetAction); //$NON-NLS-1$		

		fFindDeclarationsAction= new FindDeclarationsAction(editor);
		fFindDeclarationsAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SEARCH_DECLARATIONS_IN_WORKSPACE);
		editor.setAction("SearchDeclarationsInWorkspace", fFindDeclarationsAction); //$NON-NLS-1$		

		fFindDeclarationsInHierarchyAction= new FindDeclarationsInHierarchyAction(editor);
		fFindDeclarationsInHierarchyAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SEARCH_DECLARATIONS_IN_HIERARCHY);
		editor.setAction("SearchDeclarationsInHierarchy", fFindDeclarationsInHierarchyAction); //$NON-NLS-1$		

		fFindDeclarationsInWorkingSetAction= new FindDeclarationsInWorkingSetAction(editor);
		fFindDeclarationsInWorkingSetAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SEARCH_DECLARATIONS_IN_WORKING_SET);
		editor.setAction("SearchDeclarationsInWorkingSet", fFindDeclarationsInWorkingSetAction); //$NON-NLS-1$		

		fFindImplementorsAction= new FindImplementorsAction(editor);
		fFindImplementorsAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SEARCH_IMPLEMENTORS_IN_WORKSPACE);
		editor.setAction("SearchImplementorsInWorkspace", fFindImplementorsAction); //$NON-NLS-1$		

		fFindImplementorsInWorkingSetAction= new FindImplementorsInWorkingSetAction(editor);
		fFindImplementorsInWorkingSetAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SEARCH_IMPLEMENTORS_IN_WORKING_SET);
		editor.setAction("SearchImplementorsInWorkingSet", fFindImplementorsInWorkingSetAction); //$NON-NLS-1$		

		fOldGroup= new JavaSearchGroup(editor);
		initialize(editor.getEditorSite(), true);
	}

	private void initialize(IWorkbenchSite site, boolean isJavaEditor) {
		fSite= site;
	}

	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void fillActionBars(IActionBars actionBar) {
		super.fillActionBars(actionBar);
		setGlobalActionHandlers(actionBar);
	}
	
	private void setGlobalActionHandlers(IActionBars actionBar) {
		actionBar.setGlobalActionHandler("org.eclipse.jdt.ui.actions.ReferencesInWorkspace", fFindReferencesAction); //$NON-NLS-1$
		actionBar.setGlobalActionHandler("org.eclipse.jdt.ui.actions.ReferencesInHierarchy", fFindReferencesInHierarchyAction); //$NON-NLS-1$
		actionBar.setGlobalActionHandler("org.eclipse.jdt.ui.actions.ReferencesInWorkingSet", fFindReferencesInWorkingSetAction); //$NON-NLS-1$
		
		actionBar.setGlobalActionHandler("org.eclipse.jdt.ui.actions.ReadAccessInWorkspace", fFindReadReferencesAction); //$NON-NLS-1$
		actionBar.setGlobalActionHandler("org.eclipse.jdt.ui.actions.ReadAccessInHierarchy", fFindReadReferencesInHierarchyAction); //$NON-NLS-1$
		actionBar.setGlobalActionHandler("org.eclipse.jdt.ui.actions.ReadAccessInWorkingSet", fFindReadReferencesInWorkingSetAction); //$NON-NLS-1$

		actionBar.setGlobalActionHandler("org.eclipse.jdt.ui.actions.WriteAccessInWorkspace", fFindWriteReferencesAction); //$NON-NLS-1$
		actionBar.setGlobalActionHandler("org.eclipse.jdt.ui.actions.WriteAccessInHierarchy", fFindWriteReferencesInHierarchyAction); //$NON-NLS-1$
		actionBar.setGlobalActionHandler("org.eclipse.jdt.ui.actions.WriteAccessInWorkingSet", fFindWriteReferencesInWorkingSetAction); //$NON-NLS-1$

		actionBar.setGlobalActionHandler("org.eclipse.jdt.ui.actions.DeclarationsInWorkspace", fFindDeclarationsAction); //$NON-NLS-1$
		actionBar.setGlobalActionHandler("org.eclipse.jdt.ui.actions.DeclarationsInWorkingSet", fFindDeclarationsInWorkingSetAction); //$NON-NLS-1$
		actionBar.setGlobalActionHandler("org.eclipse.jdt.ui.actions.DeclarationsInHierarchy", fFindDeclarationsInHierarchyAction); //$NON-NLS-1$

		actionBar.setGlobalActionHandler("org.eclipse.jdt.ui.actions.ImplementorsInWorkspace", fFindImplementorsAction); //$NON-NLS-1$
		actionBar.setGlobalActionHandler("org.eclipse.jdt.ui.actions.ImplementorsInWorkingSet", fFindImplementorsInWorkingSetAction); //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void fillContextMenu(IMenuManager menu) {
		super.fillContextMenu(menu);
		fOldGroup.fill(menu, fOldContext);
	}	
}
