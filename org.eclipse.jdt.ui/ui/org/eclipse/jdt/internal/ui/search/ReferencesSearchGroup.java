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
public class ReferencesSearchGroup extends JavaSearchSubGroup  {

	public static final String GROUP_NAME= SearchMessages.getString("group.references"); //$NON-NLS-1$
	
	public ReferencesSearchGroup(IWorkbenchSite site) {
		fSite= site;
	}

	public ReferencesSearchGroup(JavaEditor editor) {
		fEditor= editor;
	}

	protected ElementSearchAction[] getActions(IWorkbenchSite site) {
		ArrayList actions= new ArrayList(ElementSearchAction.LRU_WORKINGSET_LIST_SIZE + 3);
		actions.add(new FindReferencesAction(site));
		actions.add(new FindReferencesInHierarchyAction(site));
		actions.add(new FindReferencesInWorkingSetAction(site));

		Iterator iter= ElementSearchAction.getLRUWorkingSets().sortedIterator();
		while (iter.hasNext()) {
			IWorkingSet[] workingSets= (IWorkingSet[])iter.next();
			actions.add(new WorkingSetAction(site, new FindReferencesInWorkingSetAction(site, workingSets), SearchUtil.toString(workingSets)));
		}
		return (ElementSearchAction[])actions.toArray(new ElementSearchAction[actions.size()]);
	}

	protected ElementSearchAction[] getActions(JavaEditor editor) {
		ArrayList actions= new ArrayList(ElementSearchAction.LRU_WORKINGSET_LIST_SIZE + 3);
		actions.add(new FindReferencesAction(editor));
		actions.add(new FindReferencesInHierarchyAction(editor));
		actions.add(new FindReferencesInWorkingSetAction(editor));

		Iterator iter= ElementSearchAction.getLRUWorkingSets().sortedIterator();
		while (iter.hasNext()) {
			IWorkingSet[] workingSets= (IWorkingSet[])iter.next();
			actions.add(new WorkingSetAction(editor, new FindReferencesInWorkingSetAction(editor, workingSets), SearchUtil.toString(workingSets)));
		}
		return (ElementSearchAction[])actions.toArray(new ElementSearchAction[actions.size()]);
	}
	
	protected String getName() {
		return GROUP_NAME;
	}
}
