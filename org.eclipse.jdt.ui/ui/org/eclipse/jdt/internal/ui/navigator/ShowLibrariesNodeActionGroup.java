/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.navigator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.StructuredViewer;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.navigator.IExtensionStateModel;

import org.eclipse.jdt.internal.ui.navigator.IExtensionStateConstants.Values;
import org.eclipse.jdt.internal.ui.packageview.PackagesMessages;

/**
 * Adds action to toggle display of libraries node.
 *
 * @since 3.12
 */
public class ShowLibrariesNodeActionGroup extends ActionGroup {

	private IExtensionStateModel fStateModel;

	private StructuredViewer fStructuredViewer;

	private boolean fHasContributedToViewMenu= false;

	private IAction fShowLibrariesNode= null;

	private IContributionItem fShowLibrariesNodeItem= null;

	private class ShowLibrariesNodeAction extends Action {

		public ShowLibrariesNodeAction() {
			super(PackagesMessages.LayoutActionGroup_show_libraries_in_group, AS_CHECK_BOX);
			setChecked(fStateModel.getBooleanProperty(Values.IS_LIBRARIES_NODE_SHOWN));
		}

		/*
		 * @see org.eclipse.jface.action.IAction#run()
		 */
		@Override
		public void run() {
			boolean showLibrariesNode= isChecked();
			if (fStateModel.getBooleanProperty(Values.IS_LIBRARIES_NODE_SHOWN) != showLibrariesNode) {
				fStateModel.setBooleanProperty(Values.IS_LIBRARIES_NODE_SHOWN, showLibrariesNode);

				fStructuredViewer.getControl().setRedraw(false);
				try {
					fStructuredViewer.refresh();
				} finally {
					fStructuredViewer.getControl().setRedraw(true);
				}
			}
		}
	}

	public ShowLibrariesNodeActionGroup(StructuredViewer structuredViewer,
			IExtensionStateModel stateModel) {
		super();
		fStateModel= stateModel;
		fStructuredViewer= structuredViewer;
	}

	@Override
	public void fillActionBars(IActionBars actionBars) {
		if (!fHasContributedToViewMenu) {
			IMenuManager viewMenu= actionBars.getMenuManager();
			if (fShowLibrariesNodeItem == null) {
				fShowLibrariesNodeItem= new ActionContributionItem(fShowLibrariesNode);
			}
			viewMenu.appendToGroup(CommonLayoutActionGroup.LAYOUT_GROUP_NAME, fShowLibrariesNodeItem);
			fHasContributedToViewMenu= true;
		}
	}

	public void unfillActionBars(IActionBars actionBars) {
		if (fHasContributedToViewMenu) {
			if (fShowLibrariesNodeItem != null) {
				actionBars.getMenuManager().remove(fShowLibrariesNodeItem);
				fShowLibrariesNodeItem.dispose();
				fShowLibrariesNodeItem= null;
			}
			fHasContributedToViewMenu= false;
		}
	}

	public void setShowLibrariesNode(boolean showLibrariesNode) {
		if (fShowLibrariesNode == null) {
			fShowLibrariesNode= new ShowLibrariesNodeAction();
		}
		fShowLibrariesNode.setChecked(showLibrariesNode);
	}
}
