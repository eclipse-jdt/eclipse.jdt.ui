package org.eclipse.jdt.internal.ui;/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */


import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import org.eclipse.debug.ui.IDebugUIConstants;

import org.eclipse.jdt.ui.JavaUI;

public class JavaPerspectiveFactory implements IPerspectiveFactory {
		
	/**
	 * Constructs a new Default layout engine.
	 */
	public JavaPerspectiveFactory() {
		super();
	}

	public void createInitialLayout(IPageLayout layout) {
 		String editorArea = layout.getEditorArea();
		
		IFolderLayout folder= layout.createFolder("left", IPageLayout.LEFT, (float)0.25, editorArea);
		folder.addView(IPageLayout.ID_RES_NAV);
		folder.addView(JavaUI.ID_TYPE_HIERARCHY);
		folder.addView(JavaUI.ID_PACKAGES);

		layout.addView(IPageLayout.ID_TASK_LIST, IPageLayout.BOTTOM, (float) 0.75, editorArea);
		layout.addView(IPageLayout.ID_OUTLINE, IPageLayout.RIGHT, (float)0.75, editorArea);
		
		layout.addActionSet(IDebugUIConstants.DEBUG_ACTION_SET);
		layout.addActionSet(JavaUI.ID_ACTION_SET);
		
		// views - java
		layout.addShowViewShortcut("org.eclipse.jdt.ui.PackageExplorer");
		layout.addShowViewShortcut("org.eclipse.jdt.ui.TypeHierarchy");
		
		// views - searching
		layout.addShowViewShortcut("org.eclipse.search.SearchResultView");
		
		// views - debugging
		layout.addShowViewShortcut("org.eclipse.dt.ui.ProcessView");
		layout.addShowViewShortcut("org.eclipse.dt.ui.ConsoleView");

		// views - standard workbench
		layout.addShowViewShortcut("org.eclipse.ui.views.ContentOutline");
		layout.addShowViewShortcut("org.eclipse.ui.views.TaskList");
		layout.addShowViewShortcut("org.eclipse.ui.views.ResourceNavigator");
		
		// new actions - Java project creation wizard
		layout.addNewWizardShortcut("org.eclipse.jdt.ui.wizards.NewProjectCreationWizard");
	}
}