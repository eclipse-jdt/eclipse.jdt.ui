/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.jdt.internal.ui.actions.*;

public class RenameParametersActionDelegate extends RefactoringActionDelegate {

	public RenameParametersActionDelegate() {
		super("Rename Method Parameters", "Operation unavailable on the current selection. Select a method that has parameters.");
	}
	
	/* (non-Javadoc)
	 * Method declared in IActionDelegate
	 */
	public void init(IWorkbenchWindow window) {
		super.init(window);
		StructuredSelectionProvider provider= StructuredSelectionProvider.createFrom(window.getSelectionService());
		initPossibleTargets(new RefactoringAction[] {
			RefactoringGroup.createRenameParametersAction(provider)		
		});
	}
}

