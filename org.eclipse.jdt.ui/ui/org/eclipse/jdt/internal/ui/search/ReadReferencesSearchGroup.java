/*
 * (c) Copyright IBM Corp. 2000, 2002.
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
public class ReadReferencesSearchGroup extends JavaSearchSubGroup  {

	public static final String GROUP_NAME= SearchMessages.getString("group.readReferences"); //$NON-NLS-1$

	public ReadReferencesSearchGroup(IWorkbenchSite site) {
		fSite= site;
	}

	public ReadReferencesSearchGroup(JavaEditor editor) {
		fEditor= editor;
	}

	protected JavaElementSearchAction[] getActions(IWorkbenchSite site) {
		ArrayList actions= new ArrayList(JavaElementSearchAction.LRU_WORKINGSET_LIST_SIZE + 3);
		actions.add(new FindReadReferencesAction(site));
		actions.add(new FindReadReferencesInHierarchyAction(site));
		actions.add(new FindReadReferencesInWorkingSetAction(site));

		Iterator iter= JavaElementSearchAction.getLRUWorkingSets().sortedIterator();
		while (iter.hasNext()) {
			IWorkingSet[] workingSets= (IWorkingSet[])iter.next();
			actions.add(new WorkingSetAction(site, new FindReadReferencesInWorkingSetAction(site, workingSets), SearchUtil.toString(workingSets)));
		}
		return (JavaElementSearchAction[])actions.toArray(new JavaElementSearchAction[actions.size()]);
	}

	protected JavaElementSearchAction[] getActions(JavaEditor editor) {
		ArrayList actions= new ArrayList(JavaElementSearchAction.LRU_WORKINGSET_LIST_SIZE + 3);
		actions.add(new FindReadReferencesAction(editor));
		actions.add(new FindReadReferencesInHierarchyAction(editor));
		actions.add(new FindReadReferencesInWorkingSetAction(editor));

		Iterator iter= JavaElementSearchAction.getLRUWorkingSets().sortedIterator();
		while (iter.hasNext()) {
			IWorkingSet[] workingSets= (IWorkingSet[])iter.next();
			actions.add(new WorkingSetAction(editor, new FindReadReferencesInWorkingSetAction(editor, workingSets), SearchUtil.toString(workingSets)));
		}
		return (JavaElementSearchAction[])actions.toArray(new JavaElementSearchAction[actions.size()]);
	}
	
	protected String getName() {
		return GROUP_NAME;
	}
}
