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
public class WriteReferencesSearchGroup extends JavaSearchSubGroup  {

	public static final String GROUP_NAME= SearchMessages.getString("group.writeReferences"); //$NON-NLS-1$
	
	public WriteReferencesSearchGroup(IWorkbenchSite site) {
		fSite= site;
	}

	public WriteReferencesSearchGroup(JavaEditor editor) {
		fEditor= editor;
	}

	protected FindAction[] getActions(IWorkbenchSite site) {
		ArrayList actions= new ArrayList(FindAction.LRU_WORKINGSET_LIST_SIZE + 3);
		actions.add(new FindWriteReferencesAction(site));
		actions.add(new FindWriteReferencesInHierarchyAction(site));
		actions.add(new FindWriteReferencesInWorkingSetAction(site));

		Iterator iter= FindAction.getLRUWorkingSets().sortedIterator();
		while (iter.hasNext()) {
			IWorkingSet[] workingSets= (IWorkingSet[])iter.next();
			actions.add(new WorkingSetFindAction(site, new FindWriteReferencesInWorkingSetAction(site, workingSets), SearchUtil.toString(workingSets)));
		}
		return (FindAction[])actions.toArray(new FindAction[actions.size()]);
	}

	protected FindAction[] getActions(JavaEditor editor) {
		ArrayList actions= new ArrayList(FindAction.LRU_WORKINGSET_LIST_SIZE + 3);
		actions.add(new FindWriteReferencesAction(editor));
		actions.add(new FindWriteReferencesInHierarchyAction(editor));
		actions.add(new FindWriteReferencesInWorkingSetAction(editor));

		Iterator iter= FindAction.getLRUWorkingSets().sortedIterator();
		while (iter.hasNext()) {
			IWorkingSet[] workingSets= (IWorkingSet[])iter.next();
			actions.add(new WorkingSetFindAction(editor, new FindWriteReferencesInWorkingSetAction(editor, workingSets), SearchUtil.toString(workingSets)));
		}
		return (FindAction[])actions.toArray(new FindAction[actions.size()]);
	}
	
	protected String getName() {
		return GROUP_NAME;
	}
}
