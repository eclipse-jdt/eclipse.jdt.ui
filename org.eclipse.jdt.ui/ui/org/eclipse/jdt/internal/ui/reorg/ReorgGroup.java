/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

package org.eclipse.jdt.internal.ui.reorg;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jdt.ui.IContextMenuConstants;

import org.eclipse.jdt.internal.ui.actions.ContextMenuGroup;
import org.eclipse.jdt.internal.ui.actions.GroupContext;

public class ReorgGroup extends ContextMenuGroup {

	private static final String GROUP_NAME= IContextMenuConstants.GROUP_REORGANIZE;

	private ReorgAction[] fActions;
		
	public void fill(IMenuManager manager, GroupContext context) {
		createActions(context.getSelectionProvider());
		
		for (int i= 0; i < fActions.length; i++) {
			ReorgAction action= fActions[i];
			if (action.isEnabled())
				manager.appendToGroup(GROUP_NAME, action);
		}
	}
	
	private void createActions(ISelectionProvider provider) {
		if (fActions != null)
			return;
			
		fActions= new ReorgAction[] {	
			new RenameAction(provider),
			new CopyAction(provider),
			new MoveAction(provider),
			new DeleteAction(provider)
		};
		
		ISelection sel= provider.getSelection();
		if (sel instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection= (IStructuredSelection)sel;
			for (int i= 0; i < fActions.length; i++) {
				fActions[i].selectionChanged(structuredSelection);
			}
		}
	}
}