/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.ui.IWorkbenchWindow;

import org.eclipse.jdt.internal.ui.actions.StructuredSelectionProvider;

public class PullUpActionDelegate extends RefactoringActionDelegate {

	public PullUpActionDelegate() {
		super(RefactoringMessages.getString("PullUpActionDelegate.pull_Up"), RefactoringMessages.getString("PullUpActionDelegate.unavailable")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/* (non-Javadoc)
	 * Method declared in IActionDelegate
	 */
	public void init(IWorkbenchWindow window) {
		super.init(window);
		StructuredSelectionProvider provider= StructuredSelectionProvider.createFrom(window.getSelectionService());
		initPossibleTargets(new RefactoringAction[] {
			RefactoringGroup.createPullUpAction(provider)
		});
	}
}

