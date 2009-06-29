/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.callhierarchy;

import java.util.Iterator;

import org.eclipse.swt.widgets.TreeItem;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;
import org.eclipse.jdt.internal.corext.callhierarchy.RealCallers;

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
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {
		TreeItem[] items= fCallHierarchyViewer.getTree().getSelection();
		for (int i= 0; i < items.length; i++) {
			TreeItem parent= items[i].getParentItem();
				if (!items[i].isDisposed())
					items[i].dispose();
				while (parent != null && parent.getItems().length  == 0) {// remove all the parent nodes whose children are disposed.
						TreeItem item= parent;
						parent= parent.getParentItem();
						item.dispose();
				}
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
		MethodWrapper[] wrappers= new MethodWrapper[selection.size()];
		int i= 0;
		for (Iterator iter= selection.iterator(); iter.hasNext();i++) {
			Object element= iter.next();
			if (element instanceof RealCallers || !(element instanceof MethodWrapper))//takes care of '...' node
				return false;
			wrappers[i]= (MethodWrapper)element;
			for (int j= 0; j < i; j++) {
				MethodWrapper parent= wrappers[j].getParent();
				while (parent != null) {
					if (wrappers[i] == parent) {
						return false;// disable if element is a parent of other selected elements
					}
					parent= parent.getParent();
				}
				MethodWrapper parentElement= wrappers[i].getParent();
				while (parentElement != null) {
					if (parentElement == wrappers[j]) {
						return false;// disable if element is a child of other selected elements
					}
					parentElement= parentElement.getParent();
				}

			}
			TreeItem[] items= fCallHierarchyViewer.getTree().getSelection();
			for (int k= 0; k < items.length; k++) {
				if (items[k].getExpanded()){
					TreeItem[] children= items[k].getItems();
					if (children.length == 1 && !(children[0].getData() instanceof MethodWrapper))
						return false;// Do not add action if children are still being fetched for that node
				}
			}
		}
		return true;
	}
}
