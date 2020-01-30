/*******************************************************************************
 * Copyright (c) 2009, 2013 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.callhierarchy;

import java.util.Iterator;

import org.eclipse.swt.widgets.TreeItem;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;


/**
 * This action removes a single node from the Call Hierarchy view.
 *
 * @since 3.6
 */
class RemoveFromViewAction extends Action{


	/**
	 * The Call Hierarchy view part.
	 */
	private CallHierarchyViewPart fPart;

	/**
	 * The Call Hierarchy viewer.
	 */
	private CallHierarchyViewer fCallHierarchyViewer;

	/**
	 * Creates the hide single node action.
	 *
	 * @param part the call hierarchy view part
	 * @param viewer the call hierarchy viewer
	 */
	public RemoveFromViewAction(CallHierarchyViewPart part, CallHierarchyViewer viewer) {
		fPart= part;
		fCallHierarchyViewer= viewer;
		setText(CallHierarchyMessages.RemoveFromViewAction_removeFromView_text);
		setDescription(CallHierarchyMessages.RemoveFromViewAction_removeFromView_description);
		setToolTipText(CallHierarchyMessages.RemoveFromViewAction_removeFromView_tooltip);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.CALL_HIERARCHY_REMOVE_FROM_VIEW_ACTION);

		ISharedImages workbenchImages= PlatformUI.getWorkbench().getSharedImages();
		setDisabledImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_ELCL_REMOVE_DISABLED));
		setImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_ELCL_REMOVE));
		setHoverImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_ELCL_REMOVE));
	}

	/**
	 * Removes the selected nodes from the view with out modifying the underlying view input. Thus
	 * on refresh of the view the removed nodes are also brought back to the view.
	 *
	 * @see org.eclipse.jface.action.Action#run()
	 * @since 3.9
	 */
	@Override
	public void run() {
		for (TreeItem item : fCallHierarchyViewer.getTree().getSelection()) {
			item.dispose();
		}
	}

	/**
	 * Gets the selection from the call hierarchy view part.
	 *
	 * @return the current selection
	 */
	private ISelection getSelection() {
		return fPart.getSelection();
	}

	/**
	 * Checks whether this action can be added for the selected element in the call hierarchy.
	 *
	 * @return <code> true</code> if the action can be added, <code>false</code> otherwise
	 */
	protected boolean canActionBeAdded() {
		IStructuredSelection selection= (IStructuredSelection)getSelection();
		if (selection.isEmpty())
			return false;

		Iterator<?> iter= selection.iterator();
		while (iter.hasNext()) {
			Object element= iter.next();
			if (!(element instanceof MethodWrapper))//takes care of '...' node
				return false;
		}

		for (TreeItem item : fCallHierarchyViewer.getTree().getSelection()) {
			if (!checkForChildren(item)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Checks whether the children are being fetched for a node recursively.
	 *
	 * @param item the parent node
	 * @return <code>false</code> when children are currently being fetched for a node,
	 *         <code>true</code> otherwise
	 */
	private boolean checkForChildren(TreeItem item) {
		TreeItem[] children= item.getItems();
		if (children.length == 1) {
			Object data= children[0].getData();
			if (!(data instanceof MethodWrapper) && data != null)
				return false; // Do not add action if children are still being fetched for that node or if it's only JFace's dummy node.
		}
		for (TreeItem child : children) {
			if (!checkForChildren(child)) {
				return false;
			}
		}
		return true;
	}
}
