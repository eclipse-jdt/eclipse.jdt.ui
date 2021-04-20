/*******************************************************************************
 * Copyright (c) 2003, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.navigator;

import java.util.Arrays;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.IExtensionActivationListener;
import org.eclipse.ui.navigator.IExtensionStateModel;
import org.eclipse.ui.navigator.INavigatorActivationService;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.navigator.IExtensionStateConstants.Values;

/**
 * Contributes the following actions to the menu on behalf of the JDT content
 * extension.
 *
 * <ul>
 * <li>{@link CommonLayoutActionGroup}. Contributes the "Package Presentation>" submenu in the View's drop down menu (not right-click).</li>
 * <li>{@link ShowLibrariesNodeActionGroup}. Contributes the "Show 'Referenced Libraries' Node" action in the View's drop down menu (not right-click).</li>
 * </ul>
 */
public class JavaNavigatorViewActionProvider extends CommonActionProvider {

	private static final int HIERARCHICAL_LAYOUT= 0x1;

	private static final int FLAT_LAYOUT= 0x2;

	private static final String TAG_LAYOUT= "org.eclipse.jdt.internal.ui.navigator.layout"; //$NON-NLS-1$

	private static final String TAG_LIBRARIES_NODE= "org.eclipse.jdt.internal.ui.navigator.librariesnode"; //$NON-NLS-1$

	private IExtensionStateModel fStateModel;

	private CommonLayoutActionGroup fLayoutActionGroup;

	private ShowLibrariesNodeActionGroup fShowLibrariesNodeActionGroup;

	private ICommonActionExtensionSite fExtensionSite;

	private String fExtensionId;

	private IActionBars fActionBars;

	private boolean fEnabled= false;

	private IExtensionActivationListener fMenuUpdater= new IExtensionActivationListener() {

		@Override
		public void onExtensionActivation(String viewerId, String[] theNavigatorExtensionIds, boolean isCurrentlyActive) {

			if (fExtensionSite != null && fActionBars != null) {

				int search= Arrays.binarySearch(theNavigatorExtensionIds, fExtensionId);
				if (search > -1) {
					if (isMyViewer(viewerId)) {
						if (wasEnabled(isCurrentlyActive)) {
							fLayoutActionGroup.fillActionBars(fActionBars);
							fShowLibrariesNodeActionGroup.fillActionBars(fActionBars);
						} else if (wasDisabled(isCurrentlyActive)) {
							fLayoutActionGroup.unfillActionBars(fActionBars);
							fShowLibrariesNodeActionGroup.unfillActionBars(fActionBars);
						}
						// else no change
					}
					fEnabled= isCurrentlyActive;
				}
			}

		}

		private boolean isMyViewer(String viewerId) {
			String myViewerId= fExtensionSite.getViewSite().getId();
			return myViewerId != null && myViewerId.equals(viewerId);
		}

		private boolean wasDisabled(boolean isActive) {
			return fEnabled && !isActive;
		}

		private boolean wasEnabled(boolean isActive) {
			return !fEnabled && isActive;
		}
	};


	@Override
	public void fillActionBars(IActionBars actionBars) {
		fActionBars= actionBars;
		fLayoutActionGroup.fillActionBars(actionBars);
		fShowLibrariesNodeActionGroup.fillActionBars(actionBars);
	}

	@Override
	public void init(ICommonActionExtensionSite site) {

		fExtensionSite= site;

		fStateModel= fExtensionSite.getExtensionStateModel();
		fLayoutActionGroup= new CommonLayoutActionGroup(fExtensionSite.getStructuredViewer(), fStateModel);
		fShowLibrariesNodeActionGroup = new ShowLibrariesNodeActionGroup(fExtensionSite.getStructuredViewer(), fStateModel);

		INavigatorActivationService activationService= fExtensionSite.getContentService().getActivationService();
		activationService.addExtensionActivationListener(fMenuUpdater);

		fExtensionId= fExtensionSite.getExtensionId();

		fEnabled= true;

	}

	@Override
	public void dispose() {
		fShowLibrariesNodeActionGroup.dispose();
		fLayoutActionGroup.dispose();
		fExtensionSite.getContentService().getActivationService().removeExtensionActivationListener(fMenuUpdater);
		super.dispose();
	}

	@Override
	public void setContext(ActionContext context) {
		super.setContext(context);
	}

	@Override
	public void restoreState(IMemento memento) {
		boolean isCurrentLayoutFlat= true;
		Integer state= null;
		if (memento != null)
			state= memento.getInteger(TAG_LAYOUT);

		// If no memento try an restore from preference store
		if (state == null) {
			IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
			state= store.getInt(TAG_LAYOUT);
		}

		if (state.intValue() == FLAT_LAYOUT)
			isCurrentLayoutFlat= true;
		else
			if (state.intValue() == HIERARCHICAL_LAYOUT)
				isCurrentLayoutFlat= false;

		fStateModel.setBooleanProperty(Values.IS_LAYOUT_FLAT, isCurrentLayoutFlat);
		fLayoutActionGroup.setFlatLayout(isCurrentLayoutFlat);

		Boolean showLibrariesNodeState= null;
		if (memento != null) {
			showLibrariesNodeState= memento.getBoolean(TAG_LIBRARIES_NODE);
		}

		// If no memento try to restore from preference store
		if (showLibrariesNodeState == null) {
			IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
			showLibrariesNodeState= IPreferenceStore.STRING_DEFAULT_DEFAULT.equals(store.getString(TAG_LIBRARIES_NODE)) || store.getBoolean((TAG_LIBRARIES_NODE));
		}

		boolean showLibrariesNode = showLibrariesNodeState;
		fStateModel.setBooleanProperty(Values.IS_LIBRARIES_NODE_SHOWN, showLibrariesNode);
		fShowLibrariesNodeActionGroup.setShowLibrariesNode(showLibrariesNode);
	}

	@Override
	public void saveState(IMemento aMemento) {
		super.saveState(aMemento);
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		if (fStateModel.getBooleanProperty(Values.IS_LAYOUT_FLAT)) {
			store.setValue(TAG_LAYOUT, FLAT_LAYOUT);
		} else {
			store.setValue(TAG_LAYOUT, HIERARCHICAL_LAYOUT);
		}
		store.setValue(TAG_LIBRARIES_NODE, fStateModel.getBooleanProperty(Values.IS_LIBRARIES_NODE_SHOWN));
	}
}
