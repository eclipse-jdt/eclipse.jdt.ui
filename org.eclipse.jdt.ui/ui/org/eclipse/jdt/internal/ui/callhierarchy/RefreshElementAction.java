/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.callhierarchy;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * Action class to refresh a single element in the call hierarchy.
 *
 *@since 3.6
 */
public class RefreshElementAction extends Action {

	/**
	 * The call hierarchy viewer.
	 */
	private final CallHierarchyViewer fViewer;

	/**
	 * Creates the action to refresh a single element in the call hierarchy.
	 *
	 * @param viewer the call hierarchy viewer
	 */
	public RefreshElementAction(CallHierarchyViewer viewer) {
		fViewer= viewer;
		setText(CallHierarchyMessages.RefreshSingleElementAction_text);
		setToolTipText(CallHierarchyMessages.RefreshSingleElementAction_tooltip);
		setDescription(CallHierarchyMessages.RefreshSingleElementAction_description);
		JavaPluginImages.setLocalImageDescriptors(this, "refresh.png");//$NON-NLS-1$
		setActionDefinitionId(IWorkbenchCommandConstants.FILE_REFRESH);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.CALL_HIERARCHY_REFRESH_SINGLE_ELEMENT_ACTION);
		setEnabled(true);
	}

	/**
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public void run() {
		IStructuredSelection selection= (IStructuredSelection)getSelection();
		if (selection.isEmpty()) {
			fViewer.getPart().refresh();
			return;
		}
		List<MethodWrapper> toExpand= new ArrayList<>();
		for (Object object : selection) {
			MethodWrapper element= (MethodWrapper) object;
			boolean isExpanded= fViewer.getExpandedState(element);
			element.removeFromCache();
			if (isExpanded) {
				fViewer.setExpandedState(element, false);
				toExpand.add(element);
			}
			fViewer.refresh(element);
		}
		for (MethodWrapper elem : toExpand) {
			fViewer.setExpandedState(elem, true);
		}
	}

	/**
	 * Gets the selection from the call hierarchy view part.
	 *
	 * @return the current selection
	 */
	private ISelection getSelection() {
		return fViewer.getSelection();
	}
}
