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
package org.eclipse.jdt.internal.ui.packageview;

import org.eclipse.jface.viewers.Viewer;

import org.eclipse.ui.IWorkingSet;

import org.eclipse.jdt.internal.ui.workingsets.HistoryWorkingSetUpdater;

import org.eclipse.jdt.ui.JavaElementSorter;

public class WorkingSetAwareJavaElementSorter extends JavaElementSorter implements IParentAwareSorter {
	
	private Object fParent;
	
	public void setParent(Object parent) {
		fParent= parent;
	}
	
	public void sort(Viewer viewer, Object[] elements) {
		if (fParent instanceof IWorkingSet) {
			IWorkingSet workingSet= (IWorkingSet)fParent;
			if (HistoryWorkingSetUpdater.ID.equals(workingSet.getId()))
				return;
		}
		super.sort(viewer, elements);
	}
	
	public int compare(Viewer viewer, Object e1, Object e2) {
		IWorkingSet ws1= e1 instanceof IWorkingSet ? (IWorkingSet)e1 : null;
		IWorkingSet ws2= e2 instanceof IWorkingSet ? (IWorkingSet)e2 : null;
		if (ws1 == null || ws2 == null)
			return super.compare(viewer, e1, e2);
		return 0;
	}
}
