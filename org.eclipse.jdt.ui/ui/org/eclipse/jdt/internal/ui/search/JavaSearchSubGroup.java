/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jdt.ui.IContextMenuConstants;

import org.eclipse.jdt.internal.ui.actions.ContextMenuGroup;
import org.eclipse.jdt.internal.ui.actions.GroupContext;

/**
 * Contribute Java search specific menu elements.
 */
public abstract class JavaSearchSubGroup extends ContextMenuGroup  {
	public static final String GROUP_ID= IContextMenuConstants.GROUP_SEARCH;
	abstract protected ElementSearchAction[] getActions();
	
	abstract protected String getName();
	
	public void fill(IMenuManager manager, GroupContext context) {
		MenuManager javaSearchMM= new MenuManager(getName(), GROUP_ID);
		ISelection sel= context.getSelection();
		ElementSearchAction[] actions= getActions();
		for (int i= 0; i < actions.length; i++) {
			ElementSearchAction action= actions[i];
			if (!(sel instanceof IStructuredSelection) || action.canOperateOn((IStructuredSelection)sel))
				javaSearchMM.add(action);
		}
		
		if (!javaSearchMM.isEmpty())
			manager.appendToGroup(GROUP_ID, javaSearchMM);
	}
	public IMenuManager getMenuManagerForGroup(IStructuredSelection selection) {
		MenuManager javaSearchMM= new MenuManager(getName(), GROUP_ID); //$NON-NLS-1$
		ElementSearchAction[] actions= getActions();
		if (!selection.isEmpty()) {
			for (int i= 0; i < actions.length; i++)
			if (actions[i].canOperateOn(selection))
				javaSearchMM.add(actions[i]);
		}
		return javaSearchMM;
	}
}
