/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.reorg;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.jdt.ui.IContextMenuConstants;

import org.eclipse.jdt.internal.ui.actions.ContextMenuGroup;
import org.eclipse.jdt.internal.ui.actions.GroupContext;
import org.eclipse.jdt.internal.ui.actions.StructuredSelectionProvider;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringAction;

public class ReorgGroup extends ContextMenuGroup {
	private static final String GROUP_NAME= IContextMenuConstants.GROUP_REORGANIZE;
	private RefactoringAction[] fActions;
		
	public void fill(IMenuManager manager, GroupContext context) {
		createActions(context.getSelectionProvider());
		
		for (int i= 0; i < fActions.length; i++) {
			RefactoringAction action= fActions[i];
			action.update();
			if (action.isEnabled())
				manager.appendToGroup(GROUP_NAME, action);
		}
	}
	
	private void createActions(ISelectionProvider p) {
		StructuredSelectionProvider provider= StructuredSelectionProvider.createFrom(p);
		if (fActions != null)
			return;
			
		fActions= new RefactoringAction[] {	
			new RenameAction(provider),
			new CutSourceReferencesToClipboardAction(provider),
			new CopyAction(provider),
			new CopySourceReferencesToClipboardAction(provider),
			new MoveAction(provider),
			new PasteSourceReferencesAction(provider),
			//new DuplicateSourceReferenceAction(provider, p),
			new DeleteAction(provider),
			new DeleteSourceReferencesAction(provider)	
		};		
	}	
}