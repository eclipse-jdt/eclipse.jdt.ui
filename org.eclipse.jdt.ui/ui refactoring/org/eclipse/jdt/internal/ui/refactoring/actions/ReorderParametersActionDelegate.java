/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.ui.IWorkbenchWindow;

import org.eclipse.jdt.internal.ui.actions.StructuredSelectionProvider;

public class ReorderParametersActionDelegate extends RefactoringActionDelegate {

	public ReorderParametersActionDelegate() {
		super("Modify Method Parameters", "Operation unavailable on the current selection. Select a declaration or a reference to a source method with parameters.");
	}

	/* (non-Javadoc)
	 * Method declared in IActionDelegate
	 */
	public void init(IWorkbenchWindow window) {
		super.init(window);
		StructuredSelectionProvider provider= StructuredSelectionProvider.createFrom(window.getSelectionService());
		initPossibleTargets(new RefactoringAction[] {
			RefactoringGroup.createReorderParametersAction(provider)
		});
	}
}

