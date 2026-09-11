/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.callhierarchy;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IDialogSettings;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.search.JavaSearchScopeFactory;
import org.eclipse.jdt.internal.ui.search.SearchMessages;

/**
 * Class to manage checkboxes in a menu to limit where the search should look for callers.
 */
final class SearchInSelectionManager {
	/**
	 * Section ID for the SearchInDialog class.
	 */
	private static final String DIALOG_SETTINGS_SECTION= "SearchInDialog"; //$NON-NLS-1$

	enum Search {
		IN_SOURCES(SearchMessages.SearchPage_searchIn_sources, JavaSearchScopeFactory.SOURCES, "SearchInSources"), //$NON-NLS-1$
		IN_PROJECTS(SearchMessages.SearchPage_searchIn_projects, JavaSearchScopeFactory.PROJECTS, "SearchInProjects"), //$NON-NLS-1$
		IN_JRE(SearchMessages.SearchPage_searchIn_jre, JavaSearchScopeFactory.JRE, "SearchInJRE"), //$NON-NLS-1$
		IN_LIBS(SearchMessages.SearchPage_searchIn_libraries, JavaSearchScopeFactory.LIBS, "SearchInAppLibs"); //$NON-NLS-1$

		final String label;

		final int flag;

		final String settingKey;

		Search(String label, int flag, String settingKey) {
			this.label= label;
			this.flag= flag;
			this.settingKey= settingKey;
		}
	}


	class SearchInAction extends Action {
		final Search search;

		SearchInAction(Search search, boolean checked) {
			super(null, AS_CHECK_BOX);
			setText(search.label);
			setChecked(checked);
			this.search= search;
		}

		@Override
		public void run() {
			setScopeEnabled(search, isChecked());
			viewPart.refresh();
		}
	}

	private final CallHierarchyViewPart viewPart;

	private final IDialogSettings fSettings;

	SearchInSelectionManager(CallHierarchyViewPart viewPart) {
		this.viewPart= viewPart;
		IDialogSettings settings= JavaPlugin.getDefault().getDialogSettings().getSection(DIALOG_SETTINGS_SECTION);
		if (settings == null) {
			settings= JavaPlugin.getDefault().getDialogSettings().addNewSection(DIALOG_SETTINGS_SECTION);
			for (Search setting : Search.values()) {
				setScopeEnabled(setting, true);
			}
		}
		fSettings= settings;
	}

	boolean isScopeEnabled(Search searchType) {
		return fSettings.getBoolean(searchType.settingKey);
	}

	void setScopeEnabled(Search searchType, boolean flag) {
		fSettings.put(searchType.settingKey, flag);
	}

	/**
	 * @return the menu with the various checkboxes.
	 */
	MenuManager createSearchInMenu() {
		MenuManager searchInMenu= new MenuManager(CallHierarchyMessages.ShowSearchInDialogAction_text);
		for (Search search : Search.values()) {
			searchInMenu.add(new SearchInAction(search, isScopeEnabled(search)));
		}
		return searchInMenu;
	}

	/**
	 * @return the actually selected inclusion mask.
	 */
	int getIncludeMask() {
		int mask= 0;
		for (Search setting : Search.values()) {
			if (isScopeEnabled(setting)) {
				mask|= setting.flag;
			}
		}
		return mask;
	}

}
