/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.navigator;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.TreeViewer;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionConstants;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonViewerWorkbenchSite;
import org.eclipse.ui.navigator.IExtensionStateModel;

import org.eclipse.jdt.ui.actions.CCPActionGroup;
import org.eclipse.jdt.ui.actions.GenerateActionGroup;
import org.eclipse.jdt.ui.actions.JavaSearchActionGroup;
import org.eclipse.jdt.ui.actions.OpenAction;
import org.eclipse.jdt.ui.actions.OpenEditorActionGroup;
import org.eclipse.jdt.ui.actions.RefactorActionGroup;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.navigator.IExtensionStateConstants.Values;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.GenerateBuildPathActionGroup;

public class PackageExplorerActionProvider extends CommonActionProvider { 

	private static final int HIERARCHICAL_LAYOUT = 0x1;

	private static final int FLAT_LAYOUT = 0x2;

	private static final String TAG_LAYOUT = "layout"; //$NON-NLS-1$
 
	private CommonLayoutActionGroup fLayoutActionGroup;

	private boolean fHasFilledViewMenu = false;

	private IExtensionStateModel fStateModel;
	
	private IAction fOpenAndExpand;
	
	private OpenEditorActionGroup fOpenGroup;

	private CCPActionGroup fCCPGroup;
	
	private RefactorActionGroup fRefactorGroup; 
	
	private JavaSearchActionGroup fSearchGroup;
	
	private GenerateBuildPathActionGroup fBuildPathGroup;
	
	private GenerateActionGroup fGenerateGroup;
	
	private boolean fInViewPart = false;

	public void fillActionBars(IActionBars actionBars) {
		if (!fHasFilledViewMenu) { 
			fLayoutActionGroup.fillActionBars(actionBars);
			fHasFilledViewMenu = true;
		}
		if(fInViewPart) { 
			fOpenGroup.fillActionBars(actionBars);
			fCCPGroup.fillActionBars(actionBars); 
			fBuildPathGroup.fillActionBars(actionBars);
			fGenerateGroup.fillActionBars(actionBars);			
			fRefactorGroup.fillActionBars(actionBars);
			fRefactorGroup.retargetFileMenuActions(actionBars);
			fSearchGroup.fillActionBars(actionBars);
			
			if(fOpenAndExpand == null && fOpenGroup.getOpenAction().isEnabled()) // TODO: is not updated!
				actionBars.setGlobalActionHandler(ICommonActionConstants.OPEN, fOpenGroup.getOpenAction());
			else if(fOpenAndExpand.isEnabled())
				actionBars.setGlobalActionHandler(ICommonActionConstants.OPEN, fOpenAndExpand);
		}

	}

	public void fillContextMenu(IMenuManager menu) {

		if(fInViewPart) {  
			fCCPGroup.fillContextMenu(menu); 
			fBuildPathGroup.fillContextMenu(menu);
			fGenerateGroup.fillContextMenu(menu);
			fRefactorGroup.fillContextMenu(menu); 
			fSearchGroup.fillContextMenu(menu);
		}
	}
	
	public void init(ICommonActionExtensionSite site) {

		ICommonViewerWorkbenchSite workbenchSite = null;
		if (site.getViewSite() instanceof ICommonViewerWorkbenchSite)
			workbenchSite = (ICommonViewerWorkbenchSite) site.getViewSite();

		fStateModel = site.getExtensionStateModel(); 

		fLayoutActionGroup = new CommonLayoutActionGroup(site
				.getStructuredViewer(), fStateModel);

		if (workbenchSite != null) { 
			if (workbenchSite.getPart() != null
					&& workbenchSite.getPart() instanceof IViewPart) {
				IViewPart viewPart = (IViewPart) workbenchSite.getPart();
				
				fOpenGroup = new OpenEditorActionGroup(viewPart);
				fCCPGroup = new CCPActionGroup(viewPart);
				fRefactorGroup = new RefactorActionGroup(viewPart); 
				fGenerateGroup = new GenerateActionGroup(viewPart);
				fSearchGroup = new JavaSearchActionGroup(viewPart);
				fBuildPathGroup = new GenerateBuildPathActionGroup(viewPart);
				 
				if(site.getStructuredViewer() instanceof TreeViewer)
					fOpenAndExpand = new OpenAndExpand(workbenchSite.getSite(), (OpenAction) fOpenGroup.getOpenAction(), (TreeViewer)site.getStructuredViewer());
				fInViewPart = true;
			}

		}

	}
	
	public void setContext(ActionContext context) { 
		super.setContext(context);
		if(fInViewPart) {
			fOpenGroup.setContext(context);
			fCCPGroup.setContext(context);
			fRefactorGroup.setContext(context);
			fGenerateGroup.setContext(context);
			fSearchGroup.setContext(context);
			fBuildPathGroup.setContext(context);
		}
	}

	public void restoreState(IMemento memento) {
		super.restoreState(memento);
		restoreLayoutState(memento);
	}

	private void restoreLayoutState(IMemento memento) {
		boolean isCurrentLayoutFlat = true;
		Integer state = null;
		if (memento != null)
			state = memento.getInteger(TAG_LAYOUT);

		// If no memento try an restore from preference store
		if (state == null) {
			IPreferenceStore store = JavaPlugin.getDefault()
					.getPreferenceStore();
			state = new Integer(store.getInt(TAG_LAYOUT));
		}

		if (state.intValue() == FLAT_LAYOUT)
			isCurrentLayoutFlat = true;
		else if (state.intValue() == HIERARCHICAL_LAYOUT)
			isCurrentLayoutFlat = false;

		fStateModel.setBooleanProperty(Values.IS_LAYOUT_FLAT,
				isCurrentLayoutFlat);
		fLayoutActionGroup.setFlatLayout(isCurrentLayoutFlat);
	}

}
