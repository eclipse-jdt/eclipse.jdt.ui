/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ContextMenuGroup;
import org.eclipse.jdt.internal.ui.actions.GroupContext;
import org.eclipse.jdt.ui.IContextMenuConstants;

/**
 * Contribute Java search specific menu elements.
 */
public class JavaSearchGroup extends ContextMenuGroup  {

	private ElementSearchAction[] fActions;

	public static final String GROUP_NAME= IContextMenuConstants.GROUP_SEARCH;

	public JavaSearchGroup() {
		fActions= new ElementSearchAction[] {
			new FindReferencesAction(),
			new FindDeclarationsAction(),
			new FindHierarchyReferencesAction(),
			new FindHierarchyDeclarationsAction(),
			new FindImplementorsAction()
		};
	}

	public void fill(IMenuManager manager, GroupContext context) {
		MenuManager javaSearchMM= new MenuManager(SearchMessages.getString(GROUP_NAME), GROUP_NAME); //$NON-NLS-1$
		
		for (int i= 0; i < fActions.length; i++) {
			ElementSearchAction action= fActions[i];
			if (action.canOperateOn(context.getSelection()))
				javaSearchMM.add(action);
		}
		
		if (!javaSearchMM.isEmpty())
			manager.appendToGroup(GROUP_NAME, javaSearchMM);
	}

	public String getGroupName() {
		return GROUP_NAME;
	}
	
	public MenuManager getMenuManagerForGroup(boolean isTextSelectionEmpty) {
		MenuManager javaSearchMM= new MenuManager(SearchMessages.getString(GROUP_NAME), GROUP_NAME); //$NON-NLS-1$
		if (!isTextSelectionEmpty) {
			javaSearchMM.add(new GroupMarker(GROUP_NAME));
			for (int i= 0; i < fActions.length; i++)
				javaSearchMM.appendToGroup(GROUP_NAME, fActions[i]);
		}
		return javaSearchMM;
	}
}

