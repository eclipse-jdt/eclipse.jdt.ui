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
			manager.appendToGroup(GROUP_NAME, fActions[i]);
		}
	}
	
	private void createActions(ISelectionProvider p) {
		StructuredSelectionProvider provider= StructuredSelectionProvider.createFrom(p);
		if (fActions != null)
			return;
			
		fActions= new IRefactoringAction[] {	
			new JdtMoveAction(provider),
			createCutAction(p),
			createCopyAction(p),
			createPasteAction(p),
			//new DuplicateSourceReferenceAction(provider, p),
			new RenameAction(p),
			createDeleteAction(p)
		};		
	}	
	
	public static void addGlobalReorgActions(IActionBars actionBars, ISelectionProvider provider) {
		actionBars.setGlobalActionHandler(IWorkbenchActionConstants.COPY, createCopyAction(provider));
		actionBars.setGlobalActionHandler(IWorkbenchActionConstants.CUT, createCutAction(provider));
		actionBars.setGlobalActionHandler(IWorkbenchActionConstants.PASTE, createPasteAction(provider));
	}

	private static IRefactoringAction createCutAction(ISelectionProvider p){
		return new CutSourceReferencesToClipboardAction(p);
	}
	
	public static IRefactoringAction createCopyAction(ISelectionProvider p){
		IRefactoringAction copyResources= new CopyResourcesToClipboardAction(p);
		IRefactoringAction copySourceReferences= new CopySourceReferencesToClipboardAction(p);
		return new DualReorgAction(p, ReorgMessages.getString("ReorgGroup.copy"), ReorgMessages.getString("copyAction.description"), copyResources, copySourceReferences); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public static IRefactoringAction createPasteAction(ISelectionProvider p){
		IRefactoringAction pasteResources= new PasteResourcesFromClipboardAction(p);
		IRefactoringAction pasteSourceReferences= new PasteSourceReferencesFromClipboardAction(p);
		return new DualReorgAction(p, ReorgMessages.getString("ReorgGroup.paste"), ReorgMessages.getString("ReorgGroup.pasteAction.description"), pasteResources, pasteSourceReferences); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public static IRefactoringAction createDeleteAction(ISelectionProvider p){
		IRefactoringAction deleteResources= new DeleteResourcesAction(p);
		IRefactoringAction deleteSourceReferences= new DeleteSourceReferencesAction(p);
		return new DualReorgAction(p, ReorgMessages.getString("ReorgGroup.delete"), ReorgMessages.getString("deleteAction.description"), deleteResources, deleteSourceReferences); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
}