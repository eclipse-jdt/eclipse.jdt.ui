/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.reorg;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.core.refactoring.rename.RenameJavaProjectRefactoring;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.internal.ui.actions.ContextMenuGroup;
import org.eclipse.jdt.internal.ui.actions.GroupContext;
import org.eclipse.jdt.internal.ui.refactoring.actions.OpenRefactoringWizardAction;
import org.eclipse.jdt.internal.ui.refactoring.actions.StructuredSelectionProvider;
import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

public class ReorgGroup extends ContextMenuGroup {
	private static final String GROUP_NAME= IContextMenuConstants.GROUP_REORGANIZE;
	private ReorgAction[] fActions;
		
	public void fill(IMenuManager manager, GroupContext context) {
		createActions(context.getSelectionProvider());
		
		for (int i= 0; i < fActions.length; i++) {
			ReorgAction action= fActions[i];
			action.update();
			if (action.isEnabled())
				manager.appendToGroup(GROUP_NAME, action);
		}
	}
	
	private void createActions(ISelectionProvider p) {
		StructuredSelectionProvider provider= StructuredSelectionProvider.createFrom(p);
		if (fActions != null)
			return;
			
		fActions= new ReorgAction[] {	
			new RenameAction(provider),
			new CopyAction(provider),
			new MoveAction(provider),
			new DeleteAction(provider)
		};		
	}	
}