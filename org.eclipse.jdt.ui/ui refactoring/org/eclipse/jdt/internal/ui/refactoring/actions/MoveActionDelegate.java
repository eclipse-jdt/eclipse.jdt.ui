/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.IWorkbenchWindow;

import org.eclipse.jdt.internal.ui.actions.StructuredSelectionProvider;
import org.eclipse.jdt.internal.ui.reorg.JdtMoveAction;

public class MoveActionDelegate extends RefactoringActionDelegate {

	public MoveActionDelegate() {
	}
	
	/* (non-Javadoc)
	 * Method declared in IActionDelegate
	 */
	public void init(IWorkbenchWindow window) {
		super.init(window);
		StructuredSelectionProvider provider= StructuredSelectionProvider.createFrom(window.getSelectionService());
		initPossibleTargets(new RefactoringAction[] {
			new JdtMoveAction(provider),
			RefactoringGroup.createMoveMembersAction(provider)
		});
	}
	
	/* (non-Javadoc)
	 * Method declared in RefactoringActionDelegate
	 */
	protected boolean handleTextSelection(ITextSelection selection) {
		return false;
	}		
}