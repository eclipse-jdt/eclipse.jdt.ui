/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.actions.ActionGroup;

import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jdt.ui.actions.FindAction;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

/**
 * Contribute Java search specific menu elements.
 */
public abstract class JavaSearchSubGroup extends ActionGroup  {

	public static final String GROUP_ID= IContextMenuConstants.GROUP_SEARCH;

	abstract protected FindAction[] getActions(IWorkbenchSite site);
	abstract protected FindAction[] getActions(JavaEditor editor);

	IWorkbenchSite fSite;
	JavaEditor fEditor;
	
	abstract protected String getName();

	protected final FindAction[] getActions() {
		if (fEditor != null)
			return getActions(fEditor);
		else if (fSite != null)
			return getActions(fSite);
		return null;
	}
	
	public void fillContextMenu(IMenuManager manager) {
		MenuManager javaSearchMM= new MenuManager(getName(), GROUP_ID);
		ISelection sel= getContext().getSelection();
		FindAction[] actions= getActions();
		for (int i= 0; i < actions.length; i++) {
			FindAction action= actions[i];
			if (action.isEnabled())
				javaSearchMM.add(action);
		}
		
		if (!javaSearchMM.isEmpty())
			manager.appendToGroup(GROUP_ID, javaSearchMM);
	}
	public IMenuManager getMenuManagerForGroup() {
		MenuManager javaSearchMM= new MenuManager(getName(), GROUP_ID); //$NON-NLS-1$
		FindAction[] actions= getActions();
		ISelection selection= getContext().getSelection();
		if (selection != null && !selection.isEmpty()) {
			for (int i= 0; i < actions.length; i++) {
				if (actions[i].isEnabled())
					javaSearchMM.add(actions[i]);
			}
		}
		return javaSearchMM;
	}
}
