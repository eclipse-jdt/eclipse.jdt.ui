/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkingSet;

import org.eclipse.jdt.ui.actions.FindAction;
import org.eclipse.jdt.ui.actions.FindReadReferencesAction;
import org.eclipse.jdt.ui.actions.FindReadReferencesInHierarchyAction;
import org.eclipse.jdt.ui.actions.FindReadReferencesInWorkingSetAction;
import org.eclipse.jdt.ui.actions.WorkingSetFindAction;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

/**
 * Contribute Java search specific menu elements.
 */
public class ReadReferencesSearchGroup extends JavaSearchSubGroup  {

	public static final String GROUP_NAME= SearchMessages.getString("group.readReferences"); //$NON-NLS-1$

	public ReadReferencesSearchGroup(IWorkbenchSite site) {
		fSite= site;
	}

	public ReadReferencesSearchGroup(JavaEditor editor) {
		fEditor= editor;
	}

	protected FindAction[] getActions(IWorkbenchSite site) {
		ArrayList actions= new ArrayList(SearchUtil.LRU_WORKINGSET_LIST_SIZE + 3);
		actions.add(new FindReadReferencesAction(site));
		actions.add(new FindReadReferencesInHierarchyAction(site));
		actions.add(new FindReadReferencesInWorkingSetAction(site));

		Iterator iter= SearchUtil.getLRUWorkingSets().sortedIterator();
		while (iter.hasNext()) {
			IWorkingSet[] workingSets= (IWorkingSet[])iter.next();
			actions.add(new WorkingSetFindAction(site, new FindReadReferencesInWorkingSetAction(site, workingSets), SearchUtil.toString(workingSets)));
		}
		return (FindAction[])actions.toArray(new FindAction[actions.size()]);
	}

	protected FindAction[] getActions(JavaEditor editor) {
		ArrayList actions= new ArrayList(SearchUtil.LRU_WORKINGSET_LIST_SIZE + 3);
		actions.add(new FindReadReferencesAction(editor));
		actions.add(new FindReadReferencesInHierarchyAction(editor));
		actions.add(new FindReadReferencesInWorkingSetAction(editor));

		Iterator iter= SearchUtil.getLRUWorkingSets().sortedIterator();
		while (iter.hasNext()) {
			IWorkingSet[] workingSets= (IWorkingSet[])iter.next();
			actions.add(new WorkingSetFindAction(editor, new FindReadReferencesInWorkingSetAction(editor, workingSets), SearchUtil.toString(workingSets)));
		}
		return (FindAction[])actions.toArray(new FindAction[actions.size()]);
	}
	
	protected String getName() {
		return GROUP_NAME;
	}
}
