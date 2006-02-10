/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
	
	private static class CommonLayoutAction extends Action implements IAction {

		private final boolean fIsFlatLayout;
		private IExtensionStateModel fStateModel;
		private StructuredViewer fStructuredViewer;

		public CommonLayoutAction(StructuredViewer structuredViewer, IExtensionStateModel stateModel, boolean flat) {
			super("", AS_RADIO_BUTTON); //$NON-NLS-1$
			fStateModel = stateModel;
			fStructuredViewer = structuredViewer;
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
				fStructuredViewer.refresh();
				fStructuredViewer.getControl().setRedraw(true);
			}
		}
	}
	
	
	private boolean fHasContributedToViewMenu = false;
	private IAction fHierarchicalLayout = null;
	private IAction fFlatLayoutAction = null; 

	public CommonLayoutActionGroup(StructuredViewer structuredViewer, IExtensionStateModel stateModel) {
		super();
		IAction[] actions = createActions(structuredViewer, stateModel);
		setActions(actions, stateModel.getBooleanProperty(Values.IS_LAYOUT_FLAT) ? 0 : 1);
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
	
	
	private IAction[] createActions(StructuredViewer structuredViewer, IExtensionStateModel stateModel) {
		
		fFlatLayoutAction= new CommonLayoutAction(structuredViewer, stateModel, true);
		fFlatLayoutAction.setText(PackagesMessages.LayoutActionGroup_flatLayoutAction_label); 
		JavaPluginImages.setLocalImageDescriptors(fFlatLayoutAction, "flatLayout.gif"); //$NON-NLS-1$
		
		fHierarchicalLayout= new CommonLayoutAction(structuredViewer, stateModel, false);
		fHierarchicalLayout.setText(PackagesMessages.LayoutActionGroup_hierarchicalLayoutAction_label);	  
		JavaPluginImages.setLocalImageDescriptors(fHierarchicalLayout, "hierarchicalLayout.gif"); //$NON-NLS-1$
		
		fHierarchicalLayout.setChecked(!stateModel.getBooleanProperty(Values.IS_LAYOUT_FLAT));
		fFlatLayoutAction.setChecked(stateModel.getBooleanProperty(Values.IS_LAYOUT_FLAT));
		
		return new IAction[]{fFlatLayoutAction, fHierarchicalLayout};
	}
	
	public void setFlatLayout(boolean flatLayout) {
		fHierarchicalLayout.setChecked(!flatLayout);
		fFlatLayoutAction.setChecked(flatLayout);
	}
	 
}
