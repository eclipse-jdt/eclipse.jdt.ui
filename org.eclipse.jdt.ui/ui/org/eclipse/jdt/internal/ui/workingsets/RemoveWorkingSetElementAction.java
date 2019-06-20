/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.workingsets;

import java.util.Arrays;
import java.util.HashSet;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkingSet;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

public class RemoveWorkingSetElementAction extends SelectionDispatchAction {

	public RemoveWorkingSetElementAction(IWorkbenchSite site) {
		super(site);
		setText(WorkingSetMessages.RemoveWorkingSetElementAction_label);
	}

	@Override
	public void selectionChanged(IStructuredSelection selection) {
		IWorkingSet workingSet= getWorkingSet(selection);
		setEnabled(workingSet != null && !IWorkingSetIDs.OTHERS.equals(workingSet.getId()));
	}

	private IWorkingSet getWorkingSet(IStructuredSelection selection) {
		if (!(selection instanceof ITreeSelection))
			return null;
		ITreeSelection treeSelection= (ITreeSelection)selection;
		IWorkingSet result= null;
		for (Object element : treeSelection.toList()) {
			TreePath[] paths= treeSelection.getPathsFor(element);
			if (paths.length != 1)
				return null;
			TreePath path= paths[0];
			if (path.getSegmentCount() != 2)
				return null;
			Object candidate= path.getSegment(0);
			if (!(candidate instanceof IWorkingSet))
				return null;
			if (result == null) {
				result= (IWorkingSet)candidate;
			} else {
				if (result != candidate)
					return null;
			}
		}
		return result;
	}

	@Override
	public void run(IStructuredSelection selection) {
		IWorkingSet ws= getWorkingSet(selection);
		if (ws == null)
			return;
		HashSet<IAdaptable> elements= new HashSet<>(Arrays.asList(ws.getElements()));
		for (Object object : selection.toList()) {
			if (object instanceof IAdaptable) {
				IAdaptable[] adaptedElements= ws.adaptElements(new IAdaptable[] {(IAdaptable)object});
				if (adaptedElements.length == 1) {
					elements.remove(adaptedElements[0]);
				}
			}
		}
		ws.setElements(elements.toArray(new IAdaptable[elements.size()]));
	}
}
