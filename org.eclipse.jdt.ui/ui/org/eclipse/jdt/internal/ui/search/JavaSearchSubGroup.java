/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;

import org.eclipse.jface.util.Assert;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ContextMenuGroup;
import org.eclipse.jdt.internal.ui.actions.GroupContext;
import org.eclipse.jdt.ui.IContextMenuConstants;

/**
 * Contribute Java search specific menu elements.
 */
public abstract class JavaSearchSubGroup extends ContextMenuGroup  {

	public static final String GROUP_ID= IContextMenuConstants.GROUP_SEARCH;
	private final ElementSearchAction[] fActions;

	public JavaSearchSubGroup() {
		fActions= getActions();
	}

	abstract protected ElementSearchAction[] getActions();
	
	abstract protected String getName();
	
	public void fill(IMenuManager manager, GroupContext context) {
		MenuManager javaSearchMM= new MenuManager(getName(), GROUP_ID);
		
		for (int i= 0; i < fActions.length; i++) {
			ElementSearchAction action= fActions[i];
			if (action.canOperateOn(context.getSelection()))
				javaSearchMM.add(action);
		}
		
		if (!javaSearchMM.isEmpty())
			manager.add(javaSearchMM);
	}

	public MenuManager getMenuManagerForGroup(boolean isTextSelectionEmpty) {
		MenuManager javaSearchMM= new MenuManager(getName(), GROUP_ID); //$NON-NLS-1$
		if (!isTextSelectionEmpty) {
			for (int i= 0; i < fActions.length; i++)
				javaSearchMM.add(fActions[i]);
		}
		return javaSearchMM;
	}
}
