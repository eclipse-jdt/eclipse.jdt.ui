/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchSite;

import org.eclipse.jdt.ui.IContextMenuConstants;

import org.eclipse.jdt.internal.ui.actions.ContextMenuGroup;
import org.eclipse.jdt.internal.ui.actions.GroupContext;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

/**
 * Contribute Java search specific menu elements.
 */
public abstract class JavaSearchSubGroup extends ContextMenuGroup  {

	public static final String GROUP_ID= IContextMenuConstants.GROUP_SEARCH;

	abstract protected JavaElementSearchAction[] getActions(IWorkbenchSite site);
	abstract protected JavaElementSearchAction[] getActions(JavaEditor editor);

	IWorkbenchSite fSite;
	JavaEditor fEditor;
	
	abstract protected String getName();

	protected final JavaElementSearchAction[] getActions() {
		if (fEditor != null)
			return getActions(fEditor);
		else if (fSite != null)
			return getActions(fSite);
		return null;
	}
	
	public void fill(IMenuManager manager, GroupContext context) {
		MenuManager javaSearchMM= new MenuManager(getName(), GROUP_ID);
		ISelection sel= context.getSelection();
		JavaElementSearchAction[] actions= getActions();
		for (int i= 0; i < actions.length; i++) {
			JavaElementSearchAction action= actions[i];
			action.update(sel);
			if (!(sel instanceof IStructuredSelection) || action.isEnabled())
				javaSearchMM.add(action);
		}
		
		if (!javaSearchMM.isEmpty())
			manager.appendToGroup(GROUP_ID, javaSearchMM);
	}
	public IMenuManager getMenuManagerForGroup(IStructuredSelection selection) {
		MenuManager javaSearchMM= new MenuManager(getName(), GROUP_ID); //$NON-NLS-1$
		JavaElementSearchAction[] actions= getActions();
		if (!selection.isEmpty()) {
			for (int i= 0; i < actions.length; i++) {
				actions[i].update(selection);
				if (actions[i].isEnabled())
					javaSearchMM.add(actions[i]);
			}
		}
		return javaSearchMM;
	}
}
