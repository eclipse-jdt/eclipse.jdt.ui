/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.search.ui.IWorkingSet;

/**
 * Contribute Java search specific menu elements.
 */
public class WriteReferencesSearchGroup extends JavaSearchSubGroup  {

	public static final String GROUP_NAME= SearchMessages.getString("group.writeReferences"); //$NON-NLS-1$

	protected ElementSearchAction[] getActions() {
		ArrayList actions= new ArrayList(ElementSearchAction.LRU_WORKINGSET_LIST_SIZE + 3);
		actions.add(new FindWriteReferencesAction());
		actions.add(new FindWriteReferencesInHierarchyAction());
		actions.add(new FindWriteReferencesInWorkingSetAction());

		Iterator iter= ElementSearchAction.getLRUWorkingSets().sortedIterator();
		while (iter.hasNext()) {
			IWorkingSet workingSet= (IWorkingSet)iter.next();
			actions.add(new WorkingSetAction(new FindWriteReferencesInWorkingSetAction(workingSet), workingSet.getName()));
		}
		return (ElementSearchAction[])actions.toArray(new ElementSearchAction[actions.size()]);
	}
	
	protected String getName() {
		return GROUP_NAME;
	}
}
