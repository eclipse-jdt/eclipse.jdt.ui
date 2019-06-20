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
package org.eclipse.jdt.internal.ui.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;

public class LRUWorkingSetsList {

	private final ArrayList<IWorkingSet[]> fLRUList;
	private final int fSize;
	private final  WorkingSetsComparator fComparator= new WorkingSetsComparator();

	public LRUWorkingSetsList(int size) {
		fSize= size;
		fLRUList= new ArrayList<>(size);
	}

	public void add(IWorkingSet[] workingSets) {
		removeDeletedWorkingSets();
		IWorkingSet[] existingWorkingSets= find(fLRUList, workingSets);
		if (existingWorkingSets != null)
			fLRUList.remove(existingWorkingSets);
		else if (fLRUList.size() == fSize)
			fLRUList.remove(fSize - 1);
		fLRUList.add(0, workingSets);

	}

	public Iterator<IWorkingSet[]> iterator() {
		removeDeletedWorkingSets();
		return fLRUList.iterator();
	}

	public Iterator<IWorkingSet[]> sortedIterator() {
		removeDeletedWorkingSets();
		ArrayList<IWorkingSet[]> sortedList= new ArrayList<>(fLRUList);
		Collections.sort(sortedList, fComparator);
		return sortedList.iterator();
	}

	private void removeDeletedWorkingSets() {
		Iterator<IWorkingSet[]> iter= new ArrayList<>(fLRUList).iterator();
		while (iter.hasNext()) {
			IWorkingSet[] workingSets= iter.next();
			for (IWorkingSet workingSet : workingSets) {
				if (PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSet(workingSet.getName()) == null) {
					fLRUList.remove(workingSets);
					break;
				}
			}
		}
	}

	private IWorkingSet[] find(ArrayList<IWorkingSet[]> list, IWorkingSet[] workingSets) {
		Set<IWorkingSet> workingSetList= new HashSet<>(Arrays.asList(workingSets));
		Iterator<IWorkingSet[]> iter= list.iterator();
		while (iter.hasNext()) {
			IWorkingSet[] lruWorkingSets= iter.next();
			Set<IWorkingSet> lruWorkingSetList= new HashSet<>(Arrays.asList(lruWorkingSets));
			if (lruWorkingSetList.equals(workingSetList))
				return lruWorkingSets;
		}
		return null;
	}
}
