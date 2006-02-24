/***************************************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 **************************************************************************************************/
package org.eclipse.jdt.internal.ui.navigator;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.StructuredViewer;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonViewerWorkbenchSite;
import org.eclipse.ui.navigator.IExtensionStateModel;
import org.eclipse.ui.navigator.INavigatorContentService;

import org.eclipse.jdt.ui.actions.CCPActionGroup;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.navigator.IExtensionStateConstants.Values;

public class PackageExplorerActionProvider extends CommonActionProvider { 

	private static final int HIERARCHICAL_LAYOUT = 0x1;

	private static final int FLAT_LAYOUT = 0x2;

	private static final String TAG_LAYOUT = "layout"; //$NON-NLS-1$

//	private ViewActionGroup fViewActionGroup;

	private CommonLayoutActionGroup fLayoutActionGroup;

	private boolean fHasFilledViewMenu = false;

	private IExtensionStateModel fStateModel;

	private CCPActionGroup fCCPGroup;

	public void fillActionBars(IActionBars actionBars) {
		if (!fHasFilledViewMenu) {
//			if(fViewActionGroup != null)
//				fViewActionGroup.fillActionBars(actionBars);
			fLayoutActionGroup.fillActionBars(actionBars);
			fHasFilledViewMenu = true;
		}

	}

	public void fillContextMenu(IMenuManager menu) {

		if(fCCPGroup != null)
			fCCPGroup.fillContextMenu(menu);

	}
	
	public void init(ICommonActionExtensionSite config) {

		ICommonViewerWorkbenchSite workbenchSite = null;
		if (config.getViewSite() instanceof ICommonViewerWorkbenchSite)
			workbenchSite = (ICommonViewerWorkbenchSite) config.getViewSite();

		fStateModel = config.getExtensionStateModel();
//		WorkingSetModelManager workingSetModelManager = (WorkingSetModelManager) fStateModel
//				.getProperty(WorkingSetModelManager.INSTANCE_KEY);

		fLayoutActionGroup = new CommonLayoutActionGroup(config
				.getStructuredViewer(), fStateModel);

		if (workbenchSite != null) {
//			fViewActionGroup = new ViewActionGroup(
//					ViewActionGroup.SHOW_PROJECTS, /*workingSetModelManager,*/
//					workbenchSite.getSite());
			if (workbenchSite.getPart() != null
					&& workbenchSite.getPart() instanceof IViewPart) {
				fCCPGroup = new CCPActionGroup((IViewPart) workbenchSite
						.getPart());
			}

		}

	}

	public void init(final String extensionId, final IViewPart viewPart,
			final INavigatorContentService contentService,
			final StructuredViewer structuredViewer) {

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
