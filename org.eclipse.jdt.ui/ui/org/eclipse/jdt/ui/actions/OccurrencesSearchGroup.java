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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IKeyBindingService;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;

import org.eclipse.jdt.ui.IContextMenuConstants;

import org.eclipse.jdt.internal.ui.actions.JDTQuickMenuAction;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.search.SearchMessages;

/**
 * Action group that adds the occurrences in file actions
 * to a context menu and the global menu bar.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * <p>
 * The quick menu shortcut is currently not visible in the global sub-menu,
 * see: https://bugs.eclipse.org/bugs/show_bug.cgi?id=79162
 * </p>
 * <p>
 * The quick menu is currently not working for <code>IPage</code>s, e.g. Outline pages
 * see: https://bugs.eclipse.org/bugs/show_bug.cgi?id=53812
 * </p>
 * 
 * @since 3.1
 */
public class OccurrencesSearchGroup extends ActionGroup  {

	private static final String MENU_TEXT= SearchMessages.getString("group.occurrences"); //$NON-NLS-1$

	private class QuickAccessAction extends JDTQuickMenuAction {
		public QuickAccessAction(JavaEditor editor) {
			super(editor, IJavaEditorActionDefinitionIds.SEARCH_OCCURRENCES_IN_FILE_QUICK_MENU); //$NON-NLS-1$
		}
		protected void fillMenu(IMenuManager menu) {
			fillQuickMenu(menu);
		}
	}
	
	private static class NoActionAvailable extends Action {
		public NoActionAvailable() {
			setEnabled(false);
			setText(SearchMessages.getString("group.occurrences.quickMenu.noEntriesAvailable")); //$NON-NLS-1$
		}
	}
	
	private Action fNoActionAvailable= new NoActionAvailable(); 
	private QuickAccessAction fQuickAccessAction;
	private IKeyBindingService fKeyBindingService;

	private IWorkbenchSite fSite;
	private JavaEditor fEditor;
	private IActionBars fActionBars;
	
	private String fGroupId;

	private FindOccurrencesInFileAction fOccurrencesInFileAction;
	private FindExceptionOccurrencesAction fExceptionOccurrencesAction;
	private FindImplementOccurrencesAction fFindImplementorOccurrencesAction;

	/**
	 * Creates a new <code>ImplementorsSearchGroup</code>. The group 
	 * requires that the selection provided by the site's selection provider 
	 * is of type <code>org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the view part that owns this action group
	 */
	public OccurrencesSearchGroup(IWorkbenchSite site) {
		fSite= site;
		fGroupId= IContextMenuConstants.GROUP_SEARCH;
		
		fOccurrencesInFileAction= new FindOccurrencesInFileAction(site);
		fOccurrencesInFileAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SEARCH_OCCURRENCES_IN_FILE);
		// Need to reset the label
		fOccurrencesInFileAction.setText(SearchMessages.getString("Search.FindOccurrencesInFile.shortLabel")); //$NON-NLS-1$

		fExceptionOccurrencesAction= new FindExceptionOccurrencesAction(site);
		fExceptionOccurrencesAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SEARCH_EXCEPTION_OCCURRENCES_IN_FILE);

		fFindImplementorOccurrencesAction= new FindImplementOccurrencesAction(site);
		fFindImplementorOccurrencesAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SEARCH_IMPLEMENT_OCCURRENCES_IN_FILE);

		// register the actions as selection listeners
		ISelectionProvider provider= fSite.getSelectionProvider();
		ISelection selection= provider.getSelection();
		registerAction(fOccurrencesInFileAction, provider, selection);
		registerAction(fExceptionOccurrencesAction, provider, selection);
		registerAction(fFindImplementorOccurrencesAction, provider, selection);
		
		if (site instanceof IWorkbenchPartSite) {
			fQuickAccessAction= new QuickAccessAction(null);
			fKeyBindingService= ((IWorkbenchPartSite)site).getKeyBindingService();
			fKeyBindingService.registerAction(fQuickAccessAction);
		} else if (site instanceof IPageSite) {
			/*
			 * FIXME: Can't get key binding service for page site (e.g. Outline view)
			 * 		  see: https://bugs.eclipse.org/bugs/show_bug.cgi?id=53812
			 */  
		}
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public OccurrencesSearchGroup(JavaEditor editor) {
		fEditor= editor;
		fSite= fEditor.getSite();
		fGroupId= ITextEditorActionConstants.GROUP_FIND;

		fOccurrencesInFileAction= new FindOccurrencesInFileAction(fEditor);
		fOccurrencesInFileAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SEARCH_OCCURRENCES_IN_FILE);
		// Need to reset the label
		fOccurrencesInFileAction.setText(SearchMessages.getString("Search.FindOccurrencesInFile.shortLabel")); //$NON-NLS-1$
		fEditor.setAction("SearchOccurrencesInFile", fOccurrencesInFileAction); //$NON-NLS-1$

		fExceptionOccurrencesAction= new FindExceptionOccurrencesAction(fEditor);
		fExceptionOccurrencesAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SEARCH_EXCEPTION_OCCURRENCES_IN_FILE);
		fEditor.setAction("SearchExceptionOccurrences", fExceptionOccurrencesAction); //$NON-NLS-1$

		fFindImplementorOccurrencesAction= new FindImplementOccurrencesAction(fEditor);
		fFindImplementorOccurrencesAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SEARCH_IMPLEMENT_OCCURRENCES_IN_FILE);
		fEditor.setAction("SearchImplementOccurrences", fFindImplementorOccurrencesAction); //$NON-NLS-1$
		
		fQuickAccessAction= new QuickAccessAction(editor);
		fKeyBindingService= editor.getEditorSite().getKeyBindingService();
		fKeyBindingService.registerAction(fQuickAccessAction);
	}

	private void registerAction(SelectionDispatchAction action, ISelectionProvider provider, ISelection selection){
		action.update(selection);
		provider.addSelectionChangedListener(action);
	}

	private IAction[] getActions(ISelection sel) {
		IAction[] actions= new IAction[3];
		actions[0]= fOccurrencesInFileAction;
		actions[1]= fExceptionOccurrencesAction;
		actions[2]= fFindImplementorOccurrencesAction;
		return actions;
	}

	/* 
	 * Method declared on ActionGroup.
	 */
	public void fillContextMenu(IMenuManager manager) {
		String menuText= MENU_TEXT;
		if (fQuickAccessAction != null)
			menuText= fQuickAccessAction.addShortcut(menuText);

		MenuManager javaSearchMM= new MenuManager(menuText, IContextMenuConstants.GROUP_SEARCH);
		IAction[] actions= getActions(getContext().getSelection());
		for (int i= 0; i < actions.length; i++) {
			IAction action= actions[i];
			if (action.isEnabled())
				javaSearchMM.add(action);
		}
		
		if (!javaSearchMM.isEmpty())
			manager.appendToGroup(fGroupId, javaSearchMM);
	}
	
	private void fillQuickMenu(IMenuManager manager) {
		ISelection sel= null;
		ActionContext context= getContext();
		if (context != null)
			sel= context.getSelection();
		else {
			if (fEditor != null)
				sel= fEditor.getSelectionProvider().getSelection();
			else
				sel= fSite.getSelectionProvider().getSelection();
		}
		IAction[] actions= getActions(sel);
		for (int i= 0; i < actions.length; i++) {
			IAction action= actions[i];
			if (action.isEnabled())
				manager.add(action);
		}
		if (manager.isEmpty())
			manager.add(fNoActionAvailable);
	}

	/* 
	 * Method declared on ActionGroup.
	 */
	public void fillActionBars(IActionBars actionBars) {
		Assert.isNotNull(actionBars);
		super.fillActionBars(actionBars);
		fActionBars= actionBars;
		updateGlobalActionHandlers();
	}
	
	/* 
	 * Method declared on ActionGroup.
	 */
	public void dispose() {
		ISelectionProvider provider= fSite.getSelectionProvider();
		if (provider != null) {
			disposeAction(fFindImplementorOccurrencesAction, provider);
			disposeAction(fExceptionOccurrencesAction, provider);
			disposeAction(fOccurrencesInFileAction, provider);
		}
		super.dispose();
		fFindImplementorOccurrencesAction= null;
		fExceptionOccurrencesAction= null;
		fOccurrencesInFileAction= null;
		updateGlobalActionHandlers();
		
		if (fQuickAccessAction != null && fKeyBindingService != null)
			fKeyBindingService.unregisterAction(fQuickAccessAction);
		fKeyBindingService= null;
		fQuickAccessAction= null;
		
	}

	private void updateGlobalActionHandlers() {
		if (fActionBars != null) {
			fActionBars.setGlobalActionHandler(JdtActionConstants.FIND_OCCURRENCES_IN_FILE, fOccurrencesInFileAction);
			fActionBars.setGlobalActionHandler(JdtActionConstants.FIND_EXCEPTION_OCCURRENCES, fExceptionOccurrencesAction);
			fActionBars.setGlobalActionHandler(JdtActionConstants.FIND_IMPLEMENT_OCCURRENCES, fFindImplementorOccurrencesAction);
		}
	}

	private void disposeAction(ISelectionChangedListener action, ISelectionProvider provider) {
		if (action != null)
			provider.removeSelectionChangedListener(action);
	}
}


