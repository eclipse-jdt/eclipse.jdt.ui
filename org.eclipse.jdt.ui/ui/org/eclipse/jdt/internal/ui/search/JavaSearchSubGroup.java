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
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;

import org.eclipse.jdt.ui.IContextMenuConstants;

import org.eclipse.jdt.internal.ui.actions.GroupContext;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

/**
 * Contribute Java search specific menu elements.
 */
public abstract class JavaSearchSubGroup extends ActionGroup  {

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
	
	public void fillContextMenu(IMenuManager manager) {
		MenuManager javaSearchMM= new MenuManager(getName(), GROUP_ID);
		ISelection sel= getContext().getSelection();
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
	public IMenuManager getMenuManagerForGroup() {
		MenuManager javaSearchMM= new MenuManager(getName(), GROUP_ID); //$NON-NLS-1$
		JavaElementSearchAction[] actions= getActions();
		ActionContext context= getContext();
		ISelection selection= null;
		if (context != null)
			selection= context.getSelection();
		if (selection != null && !selection.isEmpty()) {
			for (int i= 0; i < actions.length; i++) {
				actions[i].update(selection);
				if (actions[i].isEnabled())
					javaSearchMM.add(actions[i]);
			}
		}
		return javaSearchMM;
	}
}
