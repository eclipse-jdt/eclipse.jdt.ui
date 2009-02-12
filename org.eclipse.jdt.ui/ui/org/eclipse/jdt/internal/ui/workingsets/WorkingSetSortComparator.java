/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.workingsets;

import org.eclipse.ui.IWorkingSet;

import org.eclipse.jdt.internal.ui.search.WorkingSetComparator;


/**
 * Comparator class to sort working sets, keeping the "Other Projects" working set at the top.
 * 
 * @since 3.5
 */
class WorkingSetSortComparator extends WorkingSetComparator {

	/*
	 * @See Comparator#compare(Object, Object)
	 */
	public int compare(Object o1, Object o2) {

		if (o1 instanceof IWorkingSet) {
			IWorkingSet workingSet= (IWorkingSet)o1;
			if (OthersWorkingSetUpdater.ID.equals(workingSet.getId()))
				return -1;
		}

		if (o2 instanceof IWorkingSet) {
			IWorkingSet workingSet= (IWorkingSet)o2;
			if (OthersWorkingSetUpdater.ID.equals(workingSet.getId()))
				return 1;
		}

		return super.compare(o1, o2);

	}
}
