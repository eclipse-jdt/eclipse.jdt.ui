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
package org.eclipse.jdt.internal.ui.packageview;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.actions.MultiActionGroup;

/**
 * Adds view menus to switch between flat and hierarchical layout.
 * 
 * @since 2.1
 */
class LayoutActionGroup extends MultiActionGroup {

	LayoutActionGroup(PackageExplorerPart packageExplorer) {
		super(createActions(packageExplorer), getSelectedState(packageExplorer));
	}

	/* (non-Javadoc)
	 * @see ActionGroup#fillActionBars(IActionBars)
	 */
	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		contributeToViewMenu(actionBars.getMenuManager());
	}
	
	private void contributeToViewMenu(IMenuManager viewMenu) {
		viewMenu.add(new Separator());

		// Create layout sub menu
		
		IMenuManager layoutSubMenu= new MenuManager(PackagesMessages.getString("LayoutActionGroup.label")); //$NON-NLS-1$
		final String layoutGroupName= "layout"; //$NON-NLS-1$
		Separator marker= new Separator(layoutGroupName);

		viewMenu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		viewMenu.add(marker);
		viewMenu.appendToGroup(layoutGroupName, layoutSubMenu);
		viewMenu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS+"-end"));//$NON-NLS-1$		
		addActions(layoutSubMenu);
	}

	static int getSelectedState(PackageExplorerPart packageExplorer) {
		if (packageExplorer.isFlatLayout())
			return 0;
		else
			return 1;
	}
	
	static IAction[] createActions(PackageExplorerPart packageExplorer) {
		IAction flatLayoutAction= new LayoutAction(packageExplorer, true);
		flatLayoutAction.setText(PackagesMessages.getString("LayoutActionGroup.flatLayoutAction.label")); //$NON-NLS-1$
		JavaPluginImages.setLocalImageDescriptors(flatLayoutAction, "flatLayout.gif"); //$NON-NLS-1$
		IAction hierarchicalLayout= new LayoutAction(packageExplorer, false);
		hierarchicalLayout.setText(PackagesMessages.getString("LayoutActionGroup.hierarchicalLayoutAction.label"));	  //$NON-NLS-1$
		JavaPluginImages.setLocalImageDescriptors(hierarchicalLayout, "hierarchicalLayout.gif"); //$NON-NLS-1$
		
		return new IAction[]{flatLayoutAction, hierarchicalLayout};
	}
}

class LayoutAction extends Action implements IAction {

	private boolean fIsFlatLayout;
	private PackageExplorerPart fPackageExplorer;

	public LayoutAction(PackageExplorerPart packageExplorer, boolean flat) {
		super("", AS_RADIO_BUTTON); //$NON-NLS-1$

		fIsFlatLayout= flat;
		fPackageExplorer= packageExplorer;
		if (fIsFlatLayout)
			PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.LAYOUT_FLAT_ACTION);
		else
			PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.LAYOUT_HIERARCHICAL_ACTION);
	}

	/*
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	public void run() {
		if (fPackageExplorer.isFlatLayout() != fIsFlatLayout)
			fPackageExplorer.toggleLayout();
	}
}
