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
public class ImplementorsSearchGroup extends JavaSearchSubGroup  {

	public static final String GROUP_NAME= SearchMessages.getString("group.implementors"); //$NON-NLS-1$

	protected ElementSearchAction[] getActions() {
		ArrayList actions= new ArrayList(ElementSearchAction.LRU_WORKINGSET_LIST_SIZE + 2);		
		actions.add(new FindImplementorsAction());
		actions.add(new FindImplementorsInWorkingSetAction());
			
		Iterator iter= ElementSearchAction.getLRUWorkingSets().sortedIterator();
		while (iter.hasNext()) {
			IWorkingSet workingSet= (IWorkingSet)iter.next();
			actions.add(new WorkingSetAction(new FindImplementorsInWorkingSetAction(workingSet), workingSet.getName()));
		}
		return (ElementSearchAction[])actions.toArray(new ElementSearchAction[actions.size()]);
	}
	
	protected String getName() {
		return GROUP_NAME;
	}
}

