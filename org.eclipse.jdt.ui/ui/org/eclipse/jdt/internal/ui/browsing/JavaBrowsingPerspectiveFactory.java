/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.browsing;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import org.eclipse.debug.ui.IDebugUIConstants;

import org.eclipse.search.ui.SearchUI;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class JavaBrowsingPerspectiveFactory implements IPerspectiveFactory {
	
	/**
	 * Constructs a new Default layout engine.
	 */
	public JavaBrowsingPerspectiveFactory() {
		super();
	}

	public void createInitialLayout(IPageLayout layout) {
	
		// Number of open editors
		layout.setEditorReuseThreshold(JavaBrowsingPreferencePage.editorThreshold());

		if (JavaBrowsingPreferencePage.stackHorizontal()) {
			layout.addView(JavaPlugin.ID_PROJECTS_VIEW, IPageLayout.TOP, (float)0.25, IPageLayout.ID_EDITOR_AREA);
			layout.addView(JavaPlugin.ID_PACKAGES_VIEW, IPageLayout.RIGHT, (float)0.25, JavaPlugin.ID_PROJECTS_VIEW);
			layout.addView(JavaPlugin.ID_TYPES_VIEW, IPageLayout.RIGHT, (float)0.33, JavaPlugin.ID_PACKAGES_VIEW);
			layout.addView(JavaPlugin.ID_MEMBERS_VIEW, IPageLayout.RIGHT, (float)0.50, JavaPlugin.ID_TYPES_VIEW);
		} else {		
			layout.addView(JavaPlugin.ID_PROJECTS_VIEW, IPageLayout.LEFT, (float)0.25, IPageLayout.ID_EDITOR_AREA);
			layout.addView(JavaPlugin.ID_PACKAGES_VIEW, IPageLayout.BOTTOM, (float)0.25, JavaPlugin.ID_PROJECTS_VIEW);
			layout.addView(JavaPlugin.ID_TYPES_VIEW, IPageLayout.BOTTOM, (float)0.33, JavaPlugin.ID_PACKAGES_VIEW);
			layout.addView(JavaPlugin.ID_MEMBERS_VIEW, IPageLayout.BOTTOM, (float)0.50, JavaPlugin.ID_TYPES_VIEW);
		}			

		// action sets
		layout.addActionSet(IDebugUIConstants.LAUNCH_ACTION_SET);
		layout.addActionSet(JavaUI.ID_ACTION_SET);
		layout.addActionSet(JavaUI.ID_ELEMENT_CREATION_ACTION_SET);
		layout.addActionSet(JavaUI.ID_REFACTORING_ACTION_SET);
		
		// views - java
		layout.addShowViewShortcut(JavaUI.ID_TYPE_HIERARCHY);

		// views - search		
		layout.addShowViewShortcut(SearchUI.SEARCH_RESULT_VIEW_ID);
		
		// views - debugging
		layout.addShowViewShortcut(IDebugUIConstants.ID_CONSOLE_VIEW);

		// views - standard workbench
		layout.addShowViewShortcut(IPageLayout.ID_OUTLINE);
		layout.addShowViewShortcut(IPageLayout.ID_TASK_LIST);
		layout.addShowViewShortcut(IPageLayout.ID_RES_NAV);
				
		// new actions - Java project creation wizard
		layout.addNewWizardShortcut("org.eclipse.jdt.ui.wizards.NewPackageCreationWizard"); //$NON-NLS-1$
		layout.addNewWizardShortcut("org.eclipse.jdt.ui.wizards.NewClassCreationWizard"); //$NON-NLS-1$
		layout.addNewWizardShortcut("org.eclipse.jdt.ui.wizards.NewInterfaceCreationWizard"); //$NON-NLS-1$
		layout.addNewWizardShortcut("org.eclipse.jdt.ui.wizards.NewSnippetCreationWizard");	 //$NON-NLS-1$
	}
}