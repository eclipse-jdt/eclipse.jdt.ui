/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.callhierarchy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;

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
	 * @param part the call hierarchy view part
	 * @param viewer the call hierarchy viewer
	 */
	public RefreshElementAction(CallHierarchyViewer viewer) {
		fViewer= viewer;
		setText(CallHierarchyMessages.RefreshSingleElementAction_text);
		setToolTipText(CallHierarchyMessages.RefreshSingleElementAction_tooltip);
		setDescription(CallHierarchyMessages.RefreshSingleElementAction_description);
		JavaPluginImages.setLocalImageDescriptors(this, "refresh_nav.gif");//$NON-NLS-1$
		setActionDefinitionId(IWorkbenchCommandConstants.FILE_REFRESH);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.CALL_HIERARCHY_REFRESH_SINGLE_ELEMENT_ACTION);
		setEnabled(!fViewer.getSelection().isEmpty());

		fViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				setEnabled(!fViewer.getSelection().isEmpty());
			}
		});
	}

	/**
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {
		IStructuredSelection selection= (IStructuredSelection)getSelection();
		List toExpand= new ArrayList();
		for (Iterator iter= selection.iterator(); iter.hasNext();) {
			MethodWrapper element= (MethodWrapper)iter.next();
			boolean isExpanded= fViewer.getExpandedState(element);
			element.removeFromCache();
			if (isExpanded) {
				fViewer.setExpandedState(element, false);
				toExpand.add(element);
			}
			fViewer.refresh(element);
		}
		for (Iterator iter= toExpand.iterator(); iter.hasNext();) {
			MethodWrapper elem= (MethodWrapper)iter.next();
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

	/**
	 * Returns <code>true</code> if the action can be added to the menu, <code>false</code>
	 * otherwise.
	 * 
	 * @return <code>true</code> if the action can be added to the menu, <code>false</code> otherwise
	 * @since 3.6
	 */
	protected boolean canActionBeAdded() {
		return fViewer.getSelection().isEmpty() ? false : true;
	}
}
