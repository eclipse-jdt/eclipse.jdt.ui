/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;

public class LRUWorkingSetList {

	private final ArrayList fLRUList;
	private final int fSize;
	private final  WorkingSetComparator fComparator= new WorkingSetComparator();
	
	public LRUWorkingSetList(int size) {
		fSize= size;
		fLRUList= new ArrayList(size);
	}
	
	public void add(IWorkingSet workingSet) {
		removeDeletedWorkingSets();
		if (fLRUList.contains(workingSet))
			fLRUList.remove(workingSet);
		else if (fLRUList.size() == fSize)
			fLRUList.remove(fSize - 1);
		fLRUList.add(0, workingSet);

	}
	
	public Iterator iterator() {
		removeDeletedWorkingSets();
		return fLRUList.iterator();	
	}

	public Iterator sortedIterator() {
		removeDeletedWorkingSets();
		ArrayList sortedList= new ArrayList(fLRUList);
		Collections.sort(sortedList, fComparator);
		return sortedList.iterator();	
	}
	
	private void removeDeletedWorkingSets() {
		Iterator iter= new ArrayList(fLRUList).iterator();
		while (iter.hasNext()) {
			IWorkingSet workingSet= (IWorkingSet)iter.next();
			if (PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSet(workingSet.getName()) == null)
				fLRUList.remove(workingSet);
		}
	}
}
