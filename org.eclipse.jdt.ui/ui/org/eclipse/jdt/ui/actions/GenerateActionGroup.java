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

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.actions.AddBookmarkAction;
import org.eclipse.ui.part.Page;

import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.RetargetActionIDs;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;

import org.eclipse.jdt.ui.IContextMenuConstants;

/**
 * Action group that adds the source and generate actions to a context menu and
 * action bar.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class GenerateActionGroup extends ActionGroup {
	
	private OverrideMethodsAction fOverrideMethods;
	private AddGetterSetterAction fAddGetterSetter;
	private AddUnimplementedConstructorsAction fAddUnimplementedConstructors;
	private AddJavaDocStubAction fAddJavaDocStub;
	private AddBookmarkAction fAddBookmark;
	private ExternalizeStringsAction fExternalizeStrings;
	private FindStringsToExternalizeAction fFindStringsToExternalize;
	
	/**
	 * Creates a new <code>GenerateActionGroup</code>.
	 * <p>
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * </p>
	 */
	public GenerateActionGroup(CompilationUnitEditor editor) {
		ISelectionProvider provider= editor.getSite().getSelectionProvider();
		ISelection selection= provider.getSelection();
		
		fExternalizeStrings= new ExternalizeStringsAction(editor);
		fExternalizeStrings.update(selection);
	}
	
	/**
	 * Creates a new <code>GenerateActionGroup</code>.
	 * 
	 * @param page the page that owns this action group
	 */
	public GenerateActionGroup(Page page) {
		this(page.getSite());
	}

	/**
	 * Creates a new <code>GenerateActionGroup</code>.
	 * 
	 * @param part the view part that owns this action group
	 */
	public GenerateActionGroup(IViewPart part) {
		this(part.getSite());
	}

	private GenerateActionGroup(IWorkbenchSite site) {
		ISelectionProvider provider= site.getSelectionProvider();
		ISelection selection= provider.getSelection();
		
		fOverrideMethods= new OverrideMethodsAction(site);
		fAddGetterSetter= new AddGetterSetterAction(site);
		fAddUnimplementedConstructors= new AddUnimplementedConstructorsAction(site);
		fAddJavaDocStub= new AddJavaDocStubAction(site);
		fAddBookmark= new AddBookmarkAction(site.getShell());
		fExternalizeStrings= new ExternalizeStringsAction(site);
		fFindStringsToExternalize= new FindStringsToExternalizeAction(site);
		
		fOverrideMethods.update(selection);
		fAddGetterSetter.update(selection);
		fAddUnimplementedConstructors.update(selection);	
		fAddJavaDocStub.update(selection);
		fExternalizeStrings.update(selection);
		fFindStringsToExternalize.update(selection);
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ss= (IStructuredSelection)selection;
			fAddBookmark.selectionChanged(ss);
		} else {
			fAddBookmark.setEnabled(false);
		}
		
		provider.addSelectionChangedListener(fOverrideMethods);
		provider.addSelectionChangedListener(fAddGetterSetter);
		provider.addSelectionChangedListener(fAddUnimplementedConstructors);
		provider.addSelectionChangedListener(fAddJavaDocStub);
		provider.addSelectionChangedListener(fAddBookmark);
		provider.addSelectionChangedListener(fExternalizeStrings);
		provider.addSelectionChangedListener(fFindStringsToExternalize);
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
		appendToGroup(menu, fOverrideMethods);
		appendToGroup(menu, fAddGetterSetter);
		appendToGroup(menu, fAddUnimplementedConstructors);
		appendToGroup(menu, fAddJavaDocStub);
		appendToGroup(menu, fAddBookmark);
		addSubMenu(menu);
	}

	private void setGlobalActionHandlers(IActionBars actionBar) {
		actionBar.setGlobalActionHandler(RetargetActionIDs.OVERRIDE_METHODS, fOverrideMethods);
		actionBar.setGlobalActionHandler(RetargetActionIDs.GENERATE_GETTER_SETTER, fAddGetterSetter);
		actionBar.setGlobalActionHandler(RetargetActionIDs.ADD_CONSTRUCTOR_FROM_SUPERCLASS, fAddUnimplementedConstructors);
		actionBar.setGlobalActionHandler(RetargetActionIDs.ADD_JAVA_DOC_COMMENT, fAddJavaDocStub);
		actionBar.setGlobalActionHandler(IWorkbenchActionConstants.BOOKMARK, fAddBookmark);
		actionBar.setGlobalActionHandler(RetargetActionIDs.EXTERNALIZE_STRINGS, fExternalizeStrings);
		actionBar.setGlobalActionHandler(RetargetActionIDs.FIND_STRINGS_TO_EXTERNALIZE, fFindStringsToExternalize);
	}
	
	private void addSubMenu(IMenuManager menu) {
		if (fExternalizeStrings.isEnabled() || fFindStringsToExternalize.isEnabled()) {
			IMenuManager sourceMenu= new MenuManager(ActionMessages.getString("SourceMenu.label")); //$NON-NLS-1$
			addAction(sourceMenu, fExternalizeStrings);
			addAction(sourceMenu, fFindStringsToExternalize);
			menu.appendToGroup(IContextMenuConstants.GROUP_SOURCE, sourceMenu);
		}
	}	
	
	private void appendToGroup(IMenuManager menu, IAction action) {
		if (action.isEnabled())
			menu.appendToGroup(IContextMenuConstants.GROUP_SOURCE, action);
	}	

	private void addAction(IMenuManager menu, IAction action) {
		if (action.isEnabled())
			menu.add(action);
	}	
}
