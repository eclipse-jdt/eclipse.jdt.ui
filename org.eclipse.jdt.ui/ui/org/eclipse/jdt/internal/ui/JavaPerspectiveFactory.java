package org.eclipse.jdt.internal.ui;/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */


import org.eclipse.search.ui.SearchUI;import org.eclipse.debug.ui.IDebugUIConstants;import org.eclipse.ui.IFolderLayout;import org.eclipse.ui.IPageLayout;import org.eclipse.ui.IPerspectiveFactory;import org.eclipse.jdt.ui.JavaUI;

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
		layout.addShowViewShortcut(JavaUI.ID_PACKAGES);
		layout.addShowViewShortcut(JavaUI.ID_TYPE_HIERARCHY);


		layout.addShowViewShortcut(SearchUI.SEARCH_RESULT_VIEW_ID);
		
		// views - debugging
		layout.addShowViewShortcut(IDebugUIConstants.ID_PROCESS_VIEW);
		layout.addShowViewShortcut(IDebugUIConstants.ID_CONSOLE_VIEW);

		// views - standard workbench
		layout.addShowViewShortcut(IPageLayout.ID_OUTLINE);
		layout.addShowViewShortcut(IPageLayout.ID_TASK_LIST);
		layout.addShowViewShortcut(IPageLayout.ID_RES_NAV);
				
		// new actions - Java project creation wizard
		layout.addNewWizardShortcut("org.eclipse.jdt.ui.wizards.NewPackageCreationWizard");
		layout.addNewWizardShortcut("org.eclipse.jdt.ui.wizards.NewClassCreationWizard");
		layout.addNewWizardShortcut("org.eclipse.jdt.ui.wizards.NewInterfaceCreationWizard");
		layout.addNewWizardShortcut("org.eclipse.jdt.ui.wizards.NewSnippetCreationWizard");	
	}
}