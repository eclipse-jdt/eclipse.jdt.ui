/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkingSet;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

/**
 * Contribute Java search specific menu elements.
 */
public class ImplementorsSearchGroup extends JavaSearchSubGroup  {

	public ImplementorsSearchGroup(IWorkbenchSite site) {
		fSite= site;
	}

	public ImplementorsSearchGroup(JavaEditor editor) {
		fEditor= editor;
	}

	public static final String GROUP_NAME= SearchMessages.getString("group.implementors"); //$NON-NLS-1$

	protected FindAction[] getActions(IWorkbenchSite site) {
		ArrayList actions= new ArrayList(FindAction.LRU_WORKINGSET_LIST_SIZE + 2);		
		actions.add(new FindImplementorsAction(site));
		actions.add(new FindImplementorsInWorkingSetAction(site));
			
		Iterator iter= FindAction.getLRUWorkingSets().sortedIterator();
		while (iter.hasNext()) {
			IWorkingSet[] workingSets= (IWorkingSet[])iter.next();
			actions.add(new WorkingSetFindAction(site, new FindImplementorsInWorkingSetAction(site, workingSets), SearchUtil.toString(workingSets)));
		}
		return (FindAction[])actions.toArray(new FindAction[actions.size()]);
	}

	protected FindAction[] getActions(JavaEditor editor) {
		ArrayList actions= new ArrayList(FindAction.LRU_WORKINGSET_LIST_SIZE + 2);		
		actions.add(new FindImplementorsAction(editor));
		actions.add(new FindImplementorsInWorkingSetAction(editor));
			
		Iterator iter= FindAction.getLRUWorkingSets().sortedIterator();
		while (iter.hasNext()) {
			IWorkingSet[] workingSets= (IWorkingSet[])iter.next();
			actions.add(new WorkingSetFindAction(editor, new FindImplementorsInWorkingSetAction(editor, workingSets), SearchUtil.toString(workingSets)));
		}
		return (FindAction[])actions.toArray(new FindAction[actions.size()]);
	}
	
	protected String getName() {
		return GROUP_NAME;
	}
}

