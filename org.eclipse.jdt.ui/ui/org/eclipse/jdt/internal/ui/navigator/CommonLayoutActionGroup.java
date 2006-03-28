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
package org.eclipse.jdt.internal.ui.navigator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.StructuredViewer;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.IExtensionStateModel;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.actions.MultiActionGroup;
import org.eclipse.jdt.internal.ui.navigator.IExtensionStateConstants.Values;
import org.eclipse.jdt.internal.ui.packageview.PackagesMessages;

/**
 * Adds view menus to switch between flat and hierarchical layout.
 * 
 * @since 3.2
 */
public class CommonLayoutActionGroup extends MultiActionGroup {

	private IExtensionStateModel fStateModel;
	private StructuredViewer fStructuredViewer;
	
	private boolean fHasContributedToViewMenu = false;
	private IAction fHierarchicalLayout = null;
	private IAction fFlatLayoutAction = null; 
	private IAction[] actions;
	
	private class CommonLayoutAction extends Action implements IAction {

		private final boolean fIsFlatLayout;

		public CommonLayoutAction(boolean flat) {
			super("", AS_RADIO_BUTTON); //$NON-NLS-1$
			fIsFlatLayout= flat; 
			if (fIsFlatLayout)
				PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.LAYOUT_FLAT_ACTION);
			else
				PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.LAYOUT_HIERARCHICAL_ACTION);
		}

		/*
		 * @see org.eclipse.jface.action.IAction#run()
		 */
		public void run() {
			if (fStateModel.getBooleanProperty(Values.IS_LAYOUT_FLAT) != fIsFlatLayout) {
				fStateModel.setBooleanProperty(Values.IS_LAYOUT_FLAT, fIsFlatLayout);
	  			 	
				fStructuredViewer.getControl().setRedraw(false);
				try {
					fStructuredViewer.refresh();
				} finally {
					fStructuredViewer.getControl().setRedraw(true);
				}
			}
		}
	}
	

	public CommonLayoutActionGroup(StructuredViewer structuredViewer, IExtensionStateModel stateModel) {
		super();  
		fStateModel = stateModel;
		fStructuredViewer = structuredViewer;
	}

	/* (non-Javadoc)
	 * @see ActionGroup#fillActionBars(IActionBars)
	 */
	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		if(!fHasContributedToViewMenu) {
			synchronized(this) {
				if(!fHasContributedToViewMenu) {
					fHasContributedToViewMenu = true;
					contributeToViewMenu(actionBars.getMenuManager());
				}
			}
		}
	}
	
	private void contributeToViewMenu(IMenuManager viewMenu) {
		viewMenu.add(new Separator());

		// Create layout sub menu
		
		IMenuManager layoutSubMenu= new MenuManager(PackagesMessages.LayoutActionGroup_label); 
		final String layoutGroupName= "layout"; //$NON-NLS-1$
		Separator marker= new Separator(layoutGroupName);

		viewMenu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		viewMenu.add(marker);
		viewMenu.appendToGroup(layoutGroupName, layoutSubMenu);
		viewMenu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS+"-end"));//$NON-NLS-1$		
		addActions(layoutSubMenu);
	}
	
	
	private IAction[] createActions() {
		
		fFlatLayoutAction= new CommonLayoutAction(true);
		fFlatLayoutAction.setText(PackagesMessages.LayoutActionGroup_flatLayoutAction_label); 
		JavaPluginImages.setLocalImageDescriptors(fFlatLayoutAction, "flatLayout.gif"); //$NON-NLS-1$
		
		fHierarchicalLayout= new CommonLayoutAction(false);
		fHierarchicalLayout.setText(PackagesMessages.LayoutActionGroup_hierarchicalLayoutAction_label);	  
		JavaPluginImages.setLocalImageDescriptors(fHierarchicalLayout, "hierarchicalLayout.gif"); //$NON-NLS-1$
		  
		return new IAction[]{fFlatLayoutAction, fHierarchicalLayout};
	}
	
	public void setFlatLayout(boolean flatLayout) { 
		if(actions == null) {
			actions = createActions(); 
			setActions(actions, flatLayout ? 0 /* indicates check the flat action */ : 1);
		}
		fHierarchicalLayout.setChecked(!flatLayout);
		fFlatLayoutAction.setChecked(flatLayout); 
		
	}
	 
}
