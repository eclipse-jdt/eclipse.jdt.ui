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

/**
 * A LayoutEngine tuned to editing java code.
 */
public class JavaDebugLayout implements IPerspectiveFactory {
	
	public JavaDebugLayout() {
		super();
	}

	/**
	 * Populates the layout.
	 */
	public void createInitialLayout(IPageLayout layout) {
		
 		String editorArea = layout.getEditorArea();
		
		IFolderLayout folder= layout.createFolder("left", IPageLayout.LEFT, (float)0.25, editorArea);
		folder.addView(IDebugUIConstants.ID_DEBUG_VIEW);
		folder.addView(IDebugUIConstants.ID_PROCESS_VIEW);
		folder.addView(JavaUI.ID_PACKAGES);

		
		folder= layout.createFolder("right", IPageLayout.RIGHT, (float)0.70, editorArea);
		folder.addView(IDebugUIConstants.ID_BREAKPOINT_VIEW);
		folder.addView(IPageLayout.ID_OUTLINE);

		folder= layout.createFolder("right bottom", IPageLayout.BOTTOM, (float)0.5, "right");
		folder.addView(IDebugUIConstants.ID_INSPECTOR_VIEW);
		folder.addView(IDebugUIConstants.ID_VARIABLE_VIEW);

		layout.addView(IDebugUIConstants.ID_CONSOLE_VIEW, IPageLayout.BOTTOM, (float) 0.75, editorArea);
		
		layout.addActionSet(IDebugUIConstants.DEBUG_ACTION_SET);
		layout.addActionSet(JavaUI.ID_ACTION_SET);
	}	
}