/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.workingsets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkingSet;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.ui.packageview.MultiElementSelection;
import org.eclipse.jdt.internal.ui.packageview.TreePath;

public class RemoveWorkingSetElementAction extends SelectionDispatchAction {

	public RemoveWorkingSetElementAction(IWorkbenchSite site) {
		super(site);
		setText("Remo&ve from Working Set");
	}
	
	public void selectionChanged(IStructuredSelection selection) {
		IWorkingSet workingSet= getWorkingSet(selection);
		setEnabled(workingSet != null && !OthersWorkingSetUpdater.ID.equals(workingSet.getId()));
	}

	private IWorkingSet getWorkingSet(IStructuredSelection selection) {
		if (!(selection instanceof MultiElementSelection))
			return null;
		MultiElementSelection ms= (MultiElementSelection)selection;
		List elements= ms.toList();
		IWorkingSet result= null;
		for (Iterator iter= elements.iterator(); iter.hasNext();) {
			Object element= iter.next();
			TreePath[] paths= ms.getTreePaths(element);
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
	
	public void run(IStructuredSelection selection) {
		IWorkingSet ws= getWorkingSet(selection);
		if (ws == null)
			return;
		List elements= new ArrayList(Arrays.asList(ws.getElements()));
		List selectedElements= selection.toList();
		for (Iterator iter= selectedElements.iterator(); iter.hasNext();) {
			elements.remove(iter.next());
		}
		ws.setElements((IAdaptable[])elements.toArray(new IAdaptable[elements.size()]));
	}
}
