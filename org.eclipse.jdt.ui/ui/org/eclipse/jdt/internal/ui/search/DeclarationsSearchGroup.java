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
import org.eclipse.jdt.ui.actions.FindDeclarationsAction;
import org.eclipse.jdt.ui.actions.FindDeclarationsInHierarchyAction;
import org.eclipse.jdt.ui.actions.FindDeclarationsInWorkingSetAction;
import org.eclipse.jdt.ui.actions.WorkingSetFindAction;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

/**
 * Contribute Java search specific menu elements.
 */
public class DeclarationsSearchGroup extends JavaSearchSubGroup  {

	public static final String GROUP_NAME= SearchMessages.getString("group.declarations"); //$NON-NLS-1$

	public DeclarationsSearchGroup(IWorkbenchSite site) {
		fSite= site;
	}

	public DeclarationsSearchGroup(JavaEditor editor) {
		fEditor= editor;
	}

	protected FindAction[] getActions(IWorkbenchSite site) {
		ArrayList actions= new ArrayList(SearchUtil.LRU_WORKINGSET_LIST_SIZE + 3);
		actions.add(new FindDeclarationsAction(site));
		actions.add(new FindDeclarationsInHierarchyAction(site));
		actions.add(new FindDeclarationsInWorkingSetAction(site));

		Iterator iter= SearchUtil.getLRUWorkingSets().sortedIterator();
		while (iter.hasNext()) {
			IWorkingSet[] workingSets= (IWorkingSet[])iter.next();
			actions.add(new WorkingSetFindAction(site, new FindDeclarationsInWorkingSetAction(site, workingSets), SearchUtil.toString(workingSets)));
		}
		return (FindAction[])actions.toArray(new FindAction[actions.size()]);
	}

	protected FindAction[] getActions(JavaEditor editor) {
		ArrayList actions= new ArrayList(SearchUtil.LRU_WORKINGSET_LIST_SIZE + 3);
		actions.add(new FindDeclarationsAction(editor));
		actions.add(new FindDeclarationsInHierarchyAction(editor));
		actions.add(new FindDeclarationsInWorkingSetAction(editor));

		Iterator iter= SearchUtil.getLRUWorkingSets().sortedIterator();
		while (iter.hasNext()) {
			IWorkingSet[] workingSets= (IWorkingSet[])iter.next();
			actions.add(new WorkingSetFindAction(editor, new FindDeclarationsInWorkingSetAction(editor, workingSets), SearchUtil.toString(workingSets)));
		}
		return (FindAction[])actions.toArray(new FindAction[actions.size()]);
	}

	protected String getName() {
		return GROUP_NAME;
	}
}
