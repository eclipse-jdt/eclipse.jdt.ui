/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchWindow;

import org.eclipse.jdt.internal.ui.reorg.RenameAction;
import org.eclipse.jdt.internal.ui.actions.*;

public class RenameActionDelegate extends RefactoringActionDelegate {

	public RenameActionDelegate() {
		super("Rename", "Operation unavailable on the current selection. Select a field, method or type.");
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

