/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.reorg;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;

import org.eclipse.jdt.ui.IContextMenuConstants;

import org.eclipse.jdt.internal.ui.actions.ContextMenuGroup;
import org.eclipse.jdt.internal.ui.actions.GroupContext;
import org.eclipse.jdt.internal.ui.actions.StructuredSelectionProvider;
import org.eclipse.jdt.internal.ui.refactoring.actions.IRefactoringAction;

public class ReorgGroup extends ContextMenuGroup {
	private static final String GROUP_NAME= IContextMenuConstants.GROUP_REORGANIZE;
	private IRefactoringAction[] fActions;
		
	public void fill(IMenuManager manager, GroupContext context) {
		createActions(context.getSelectionProvider());
		
		for (int i= 0; i < fActions.length; i++) {
			fActions[i].update();
			if (fActions[i].isEnabled())
				manager.appendToGroup(GROUP_NAME, fActions[i]);
		}
	}
	
	private void createActions(ISelectionProvider p) {
		StructuredSelectionProvider provider= StructuredSelectionProvider.createFrom(p);
		if (fActions != null)
			return;
			
		fActions= new IRefactoringAction[] {	
			new RenameAction(p),
			new CutSourceReferencesToClipboardAction(p),
			new CopyResourcesToClipboardAction(p),
			new CopySourceReferencesToClipboardAction(p),
			new MoveAction(provider),
			new PasteSourceReferencesAction(p),
			new PasteResourcesFromClipboardAction(p),
			//new DuplicateSourceReferenceAction(provider, p),
			new DeleteAction(p)
		};		
	}	
	
	public static void addGlobalReorgActions(IActionBars actionBars, ISelectionProvider provider) {
		actionBars.setGlobalActionHandler(IWorkbenchActionConstants.COPY, new CopySourceReferencesToClipboardAction(provider));
		actionBars.setGlobalActionHandler(IWorkbenchActionConstants.CUT, new CutSourceReferencesToClipboardAction(provider));
		actionBars.setGlobalActionHandler(IWorkbenchActionConstants.PASTE, new PasteSourceReferencesAction(provider));
	}
}