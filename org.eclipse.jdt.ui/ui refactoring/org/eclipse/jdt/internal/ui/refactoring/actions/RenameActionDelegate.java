/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchWindow;

import org.eclipse.jdt.internal.ui.reorg.RenameAction;

public class RenameActionDelegate extends RefactoringActionDelegate {

	public RenameActionDelegate() {
	}
	
	/* (non-Javadoc)
	 * Method declared in IActionDelegate
	 */
	public void init(IWorkbenchWindow window) {
		super.init(window);
		StructuredSelectionProvider provider= StructuredSelectionProvider.createFrom(window.getSelectionService());
		initPossibleTargets(new RefactoringAction[] {
			new RenameAction(provider)		
		});
	}
}

