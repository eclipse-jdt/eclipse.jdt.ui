package org.eclipse.jdt.internal.ui.search;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
import java.util.ResourceBundle;

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
			new FindHierarchyDeclarationsAction(),
			new FindImplementorsAction()
		};
	}

	public void fill(IMenuManager manager, GroupContext context) {
		for (int i= 0; i < fActions.length; i++) {
			ElementSearchAction action= fActions[i];
			if (action.canOperateOn(context.getSelection()))
				manager.appendToGroup(GROUP_NAME, action);
		}
	}

	public String getGroupName() {
		return GROUP_NAME;
	}
	
	public MenuManager getMenuManagerForGroup() {
		ResourceBundle bundle= JavaPlugin.getResourceBundle();
		MenuManager javaSearchMM= new MenuManager(bundle.getString(GROUP_NAME), GROUP_NAME);
		javaSearchMM.add(new GroupMarker(GROUP_NAME));
		for (int i= 0; i < fActions.length; i++)
			javaSearchMM.appendToGroup(GROUP_NAME, fActions[i]);
		return javaSearchMM;
	}
}

