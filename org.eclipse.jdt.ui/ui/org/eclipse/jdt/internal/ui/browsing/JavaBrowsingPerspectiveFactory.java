/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.browsing;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import org.eclipse.debug.ui.IDebugUIConstants;

import org.eclipse.search.ui.SearchUI;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class JavaBrowsingPerspectiveFactory implements IPerspectiveFactory {
	
	/*
	 * XXX: This is a workaround for: http://dev.eclipse.org/bugs/show_bug.cgi?id=13070
	 */
	static IJavaElement fgJavaElementFromAction;
	
	/**
	 * Constructs a new Default layout engine.
	 */
	public JavaBrowsingPerspectiveFactory() {
		super();
	}

	public void createInitialLayout(IPageLayout layout) {
		if (JavaBrowsingPreferencePage.stackHorizontal())
			createHorizontalLayout(layout);
		else
			createVerticalLayout(layout);

		// action sets
		layout.addActionSet(IDebugUIConstants.LAUNCH_ACTION_SET);
		layout.addActionSet(JavaUI.ID_ACTION_SET);
		layout.addActionSet(JavaUI.ID_ELEMENT_CREATION_ACTION_SET);
		
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

	private void createVerticalLayout(IPageLayout layout) {
		String relativePartId= IPageLayout.ID_EDITOR_AREA;
		int relativePos= IPageLayout.LEFT;
		
		if (shouldShowProjectsView()) {
			layout.addView(JavaPlugin.ID_PROJECTS_VIEW, IPageLayout.LEFT, (float)0.25, IPageLayout.ID_EDITOR_AREA);
			relativePartId= JavaPlugin.ID_PROJECTS_VIEW;
			relativePos= IPageLayout.BOTTOM;
		}				
		if (shouldShowPackagesView()) {
			layout.addView(JavaPlugin.ID_PACKAGES_VIEW, relativePos, (float)0.25, relativePartId);
			relativePartId= JavaPlugin.ID_PACKAGES_VIEW;
			relativePos= IPageLayout.BOTTOM;
		}
		layout.addView(JavaPlugin.ID_TYPES_VIEW, relativePos, (float)0.33, relativePartId);
		layout.addView(JavaPlugin.ID_MEMBERS_VIEW, IPageLayout.BOTTOM, (float)0.50, JavaPlugin.ID_TYPES_VIEW);
	}

	private void createHorizontalLayout(IPageLayout layout) {
		String relativePartId= IPageLayout.ID_EDITOR_AREA;		
		int relativePos= IPageLayout.TOP;
		
		if (shouldShowProjectsView()) {
			layout.addView(JavaPlugin.ID_PROJECTS_VIEW, IPageLayout.TOP, (float)0.25, IPageLayout.ID_EDITOR_AREA);
			relativePartId= JavaPlugin.ID_PROJECTS_VIEW;
			relativePos= IPageLayout.RIGHT;
		}
		if (shouldShowPackagesView()) {
			layout.addView(JavaPlugin.ID_PACKAGES_VIEW, relativePos, (float)0.25, relativePartId);
			relativePartId= JavaPlugin.ID_PACKAGES_VIEW;
			relativePos= IPageLayout.RIGHT;
		}
		layout.addView(JavaPlugin.ID_TYPES_VIEW, relativePos, (float)0.33, relativePartId);
		layout.addView(JavaPlugin.ID_MEMBERS_VIEW, IPageLayout.RIGHT, (float)0.50, JavaPlugin.ID_TYPES_VIEW);
	}
	
	private boolean shouldShowProjectsView() {
		return fgJavaElementFromAction == null || fgJavaElementFromAction.getElementType() == IJavaElement.JAVA_MODEL;
	}

	private boolean shouldShowPackagesView() {
		if (fgJavaElementFromAction == null)
			return true;
		int type= fgJavaElementFromAction.getElementType();
		return type == IJavaElement.JAVA_MODEL || type == IJavaElement.JAVA_PROJECT || type == IJavaElement.PACKAGE_FRAGMENT_ROOT;
	}

	/*
	 * XXX: This is a workaround for: http://dev.eclipse.org/bugs/show_bug.cgi?id=13070
	 */
	static void setInputFromAction(IAdaptable input) {
		if (input instanceof IJavaElement)
			fgJavaElementFromAction= (IJavaElement)input;
		else
			fgJavaElementFromAction= null;
	}
}