/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.actions;

import org.eclipse.jdt.internal.ui.actions.FindExceptionOccurrences;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.search.SearchMessages;
import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.Assert;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;

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

	private JavaEditor fEditor;
	private IWorkbenchSite fSite;

	private ReferencesSearchGroup fReferencesGroup;
	private ReadReferencesSearchGroup fReadAccessGroup;
	private WriteReferencesSearchGroup fWriteAccessGroup;
	private DeclarationsSearchGroup fDeclarationsGroup;
	private ImplementorsSearchGroup fImplementorsGroup;
	
	private FindOccurrencesInFileAction fOccurrencesInFileAction;
	private FindExceptionOccurrences fExceptionOriginatorsAction;
	
	/**
	 * Creates a new <code>JavaSearchActionGroup</code>. The group 
	 * requires that the selection provided by the part's selection provider 
	 * is of type <code>org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param part the view part that owns this action group
	 */
	public JavaSearchActionGroup(IViewPart part) {
		this(part.getViewSite());
		fOccurrencesInFileAction= new FindOccurrencesInFileAction(part);
		part.getViewSite().getSelectionProvider().addSelectionChangedListener(fOccurrencesInFileAction);
	}
	
	/**
	 * Creates a new <code>JavaSearchActionGroup</code>. The group 
	 * requires that the selection provided by the page's selection provider 
	 * is of type <code>org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param page the page that owns this action group
	 */
	public JavaSearchActionGroup(Page page) {
		this(page.getSite());
		fOccurrencesInFileAction= new FindOccurrencesInFileAction(page);
		page.getSite().getSelectionProvider().addSelectionChangedListener(fOccurrencesInFileAction);
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public JavaSearchActionGroup(JavaEditor editor) {
		Assert.isNotNull(editor);
		fEditor= editor;
		fSite= fEditor.getSite();
		
		fReferencesGroup= new ReferencesSearchGroup(fEditor);
		fReadAccessGroup= new ReadReferencesSearchGroup(fEditor);
		fWriteAccessGroup= new WriteReferencesSearchGroup(fEditor);
		fDeclarationsGroup= new DeclarationsSearchGroup(fEditor);
		fImplementorsGroup= new ImplementorsSearchGroup(fEditor);
		fOccurrencesInFileAction= new FindOccurrencesInFileAction(fEditor);
		fExceptionOriginatorsAction= new FindExceptionOccurrences(fEditor);
	}

	private JavaSearchActionGroup(IWorkbenchSite site) {
		fReferencesGroup= new ReferencesSearchGroup(site);
		fReadAccessGroup= new ReadReferencesSearchGroup(site);
		fWriteAccessGroup= new WriteReferencesSearchGroup(site);
		fDeclarationsGroup= new DeclarationsSearchGroup(site);
		fImplementorsGroup= new ImplementorsSearchGroup(site);
		fSite= site;
	}

	/* 
	 * Method declared on ActionGroup.
	 */
	public void setContext(ActionContext context) {
		fReferencesGroup.setContext(context);
		fDeclarationsGroup.setContext(context);
		fImplementorsGroup.setContext(context);
		fReadAccessGroup.setContext(context);
		fWriteAccessGroup.setContext(context);
	}

	/* 
	 * Method declared on ActionGroup.
	 */
	public void fillActionBars(IActionBars actionBar) {
		super.fillActionBars(actionBar);
		fReferencesGroup.fillActionBars(actionBar);
		fDeclarationsGroup.fillActionBars(actionBar);
		fImplementorsGroup.fillActionBars(actionBar);
		fReadAccessGroup.fillActionBars(actionBar);
		fWriteAccessGroup.fillActionBars(actionBar);
		actionBar.setGlobalActionHandler(JdtActionConstants.FIND_OCCURRENCES_IN_FILE, fOccurrencesInFileAction);
		actionBar.setGlobalActionHandler(JdtActionConstants.FIND_EXCEPTION_OCCURRENCES, fExceptionOriginatorsAction);
	}
	
	/* 
	 * Method declared on ActionGroup.
	 */
	public void fillContextMenu(IMenuManager menu) {
		super.fillContextMenu(menu);
		
		if(PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.SEARCH_USE_REDUCED_MENU)) {
			fReferencesGroup.fillContextMenu(menu);
			fDeclarationsGroup.fillContextMenu(menu);

			if (fEditor == null) {
				fImplementorsGroup.fillContextMenu(menu);
				fReadAccessGroup.fillContextMenu(menu);
				fWriteAccessGroup.fillContextMenu(menu);
				addAction(menu, IContextMenuConstants.GROUP_SEARCH, fOccurrencesInFileAction);		
			}

		} else {
			IMenuManager target= menu;
			IMenuManager searchSubMenu= null;
			if (fEditor != null) {
				String groupName= SearchMessages.getString("group.search"); //$NON-NLS-1$
				searchSubMenu= new MenuManager(groupName, ITextEditorActionConstants.GROUP_FIND);
				searchSubMenu.add(new GroupMarker(ITextEditorActionConstants.GROUP_FIND));
				target= searchSubMenu;
			}
			
			fReferencesGroup.fillContextMenu(target);
			fDeclarationsGroup.fillContextMenu(target);
			fImplementorsGroup.fillContextMenu(target);
			fReadAccessGroup.fillContextMenu(target);
			fWriteAccessGroup.fillContextMenu(target);
			
			if (searchSubMenu != null) {
				searchSubMenu.add(new Separator());
				addAction(target, fOccurrencesInFileAction);
			} else {
				addAction(target, IContextMenuConstants.GROUP_SEARCH, fOccurrencesInFileAction);
			}
			
			// no other way to find out if we have added items.
			if (searchSubMenu != null && searchSubMenu.getItems().length > 2) {		
				menu.appendToGroup(ITextEditorActionConstants.GROUP_FIND, searchSubMenu);
			}
		}
	}	

	/* 
	 * Method declared on ActionGroup.
	 */
	public void dispose() {
		fReferencesGroup.dispose();
		fDeclarationsGroup.dispose();
		fImplementorsGroup.dispose();
		fReadAccessGroup.dispose();
		fWriteAccessGroup.dispose();
		if (fOccurrencesInFileAction != null)
			fSite.getSelectionProvider().removeSelectionChangedListener(fOccurrencesInFileAction);
		super.dispose();
	}
	
	private void addAction(IMenuManager manager, IAction action) {
		if (action != null && action.isEnabled())
			manager.add(action);
	}
	
	private void addAction(IMenuManager manager, String group, IAction action) {
		if (action != null && action.isEnabled())
			manager.appendToGroup(group, action);
	}	
}
