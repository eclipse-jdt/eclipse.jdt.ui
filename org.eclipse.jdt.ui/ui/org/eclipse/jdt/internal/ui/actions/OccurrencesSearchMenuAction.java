/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.actions;

import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowPulldownDelegate2;

import org.eclipse.jdt.ui.actions.FindExceptionOccurrencesAction;
import org.eclipse.jdt.ui.actions.FindImplementOccurrencesAction;
import org.eclipse.jdt.ui.actions.FindOccurrencesInFileAction;
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.search.SearchMessages;

/**
 * <p>
 * This is required because of 
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=79162
 * and
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=137679
 * </p>
 */
public class OccurrencesSearchMenuAction implements IWorkbenchWindowPulldownDelegate2 {
	
	private static Action NO_ACTION_AVAILABLE= new Action(SearchMessages.group_occurrences_quickMenu_noEntriesAvailable) {
		public boolean isEnabled() {
			return false;
		}
	};

	private Menu fMenu;

	/**
	 * {@inheritDoc}
	 */
	public Menu getMenu(Menu parent) {
		setMenu(new Menu(parent));
		fillMenu(fMenu);
		installMenuListener();
		return fMenu;
	}

	/**
	 * {@inheritDoc}
	 */
	public Menu getMenu(Control parent) {
		setMenu(new Menu(parent));
		fillMenu(fMenu);
		installMenuListener();
		return fMenu;
	}

	/**
	 * {@inheritDoc}
	 */
	public void dispose() {
		setMenu(null);
	}

	/**
	 * {@inheritDoc}
	 */
	public void init(IWorkbenchWindow window) {
	}

	/**
	 * {@inheritDoc}
	 */
	public void run(IAction action) {
		IEditorPart activeEditor= JavaPlugin.getActivePage().getActiveEditor();
		if (!(activeEditor instanceof CompilationUnitEditor))
			return;
		
		final CompilationUnitEditor editor= (CompilationUnitEditor)activeEditor;
		
		(new JDTQuickMenuAction(editor, IJavaEditorActionDefinitionIds.SEARCH_OCCURRENCES_IN_FILE_QUICK_MENU) {
			protected void fillMenu(IMenuManager menu) {
				fillQuickMenu(menu);
			}
		}).run();

	}

	/**
	 * {@inheritDoc}
	 */
	public void selectionChanged(IAction action, ISelection selection) {
	}
	
	private void fillQuickMenu(IMenuManager manager) {
		IAction[] actions= getActions(JavaPlugin.getActiveWorkbenchWindow());
		if (actions != null) {
			boolean hasAction= false;
			for (int i= 0; i < actions.length; i++) {
				IAction action= actions[i];
				if (action.isEnabled()) {
					hasAction= true;
					manager.add(action);
				}
			}
			if (!hasAction) {
				manager.add(NO_ACTION_AVAILABLE);
			}
		} else {
			manager.add(NO_ACTION_AVAILABLE);
		}
	}
	
	/**
	 * The menu to show in the workbench menu
	 */
	private void fillMenu(Menu menu) {
		IAction[] actions= getActions(JavaPlugin.getActiveWorkbenchWindow());
		if (actions != null) {
			boolean hasAction= false;
			for (int i= 0; i < actions.length; i++) {
				IAction action= actions[i];
				if (action.isEnabled()) {
					hasAction= true;
					ActionContributionItem item= new ActionContributionItem(action);
					item.fill(menu, -1);
				}
			}
			if (!hasAction) {
				ActionContributionItem item= new ActionContributionItem(NO_ACTION_AVAILABLE);
				item.fill(menu, -1);
			}
		} else {
			ActionContributionItem item= new ActionContributionItem(NO_ACTION_AVAILABLE);
			item.fill(menu, -1);
		}
	}
	
	private IAction[] getActions(IWorkbenchWindow window) {
		IWorkbenchPage activePage= window.getActivePage();
		if (activePage == null)
			return null;
		
		IWorkbenchPart activePart= activePage.getActivePart();
		if (activePart == null)
			return null;
		
		if (activePart instanceof JavaEditor) {
			return getActions((JavaEditor)activePart);
		} else {
			return getActions(activePart.getSite());
		}
	}

	private IAction[] getActions(IWorkbenchPartSite site) {
		FindOccurrencesInFileAction findIdentifier= new FindOccurrencesInFileAction(site);
		FindExceptionOccurrencesAction findExceptions= new FindExceptionOccurrencesAction(site);
		FindImplementOccurrencesAction findImplement= new FindImplementOccurrencesAction(site);
		
		ISelectionProvider selectionProvider= site.getSelectionProvider();
		if (selectionProvider == null)
			return null;
		
		ISelection selection= selectionProvider.getSelection();
		init(findIdentifier, selection);
		init(findExceptions, selection);
		init(findImplement, selection);
		return new IAction[] {
				findIdentifier,
				findExceptions,
				findImplement
		};
	}

	private IAction[] getActions(JavaEditor editor) {
		FindOccurrencesInFileAction findIdentifier= new FindOccurrencesInFileAction(editor);
		FindExceptionOccurrencesAction findExceptions= new FindExceptionOccurrencesAction(editor);
		FindImplementOccurrencesAction findImplement= new FindImplementOccurrencesAction(editor);
		
		ISelectionProvider selectionProvider= editor.getSelectionProvider();
		if (selectionProvider == null)
			return null;
		
		ISelection selection= selectionProvider.getSelection();
		init(findIdentifier, selection);
		init(findExceptions, selection);
		init(findImplement, selection);
		
		editor.setAction("SearchOccurrencesInFile", findIdentifier); //$NON-NLS-1$
		editor.setAction("SearchExceptionOccurrences", findExceptions); //$NON-NLS-1$
		editor.setAction("SearchImplementOccurrences", findImplement); //$NON-NLS-1$
		
		return new IAction[] {
				findIdentifier,
				findExceptions,
				findImplement
		};
	}

	private void init(FindImplementOccurrencesAction findImplement, ISelection selection) {
		findImplement.update(selection);
		findImplement.setActionDefinitionId(IJavaEditorActionDefinitionIds.SEARCH_IMPLEMENT_OCCURRENCES_IN_FILE);
	}

	private void init(FindExceptionOccurrencesAction findExceptions, ISelection selection) {
		findExceptions.update(selection);
		findExceptions.setActionDefinitionId(IJavaEditorActionDefinitionIds.SEARCH_EXCEPTION_OCCURRENCES_IN_FILE);
	}

	private void init(FindOccurrencesInFileAction findIdentifier, ISelection selection) {
		findIdentifier.update(selection);
		findIdentifier.setActionDefinitionId(IJavaEditorActionDefinitionIds.SEARCH_OCCURRENCES_IN_FILE);
		findIdentifier.setText(SearchMessages.Search_FindOccurrencesInFile_shortLabel);
	}

	private void setMenu(Menu menu) {
		if (fMenu != null) {
			fMenu.dispose();
		}
		fMenu = menu;
	}
	
	private void installMenuListener() {
		fMenu.addMenuListener(new MenuAdapter() {
			public void menuShown(MenuEvent e) {
				Menu m = (Menu)e.widget;
				MenuItem[] items = m.getItems();
				for (int i=0; i < items.length; i++) {
					items[i].dispose();
				}
				fillMenu(m);
			}
		});
	}
}
