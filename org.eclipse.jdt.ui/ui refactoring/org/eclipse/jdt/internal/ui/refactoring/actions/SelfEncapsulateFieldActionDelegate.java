/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.ui.IWorkbenchWindow;

import org.eclipse.jdt.internal.ui.actions.StructuredSelectionProvider;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;

public class SelfEncapsulateFieldActionDelegate extends RefactoringActionDelegate {

	public SelfEncapsulateFieldActionDelegate() {
		super(RefactoringMessages.getString("SelfEncapsulateFieldActionDelegate.sef"), RefactoringMessages.getString("SelfEncapsulateFieldActionDelegate.unavailable")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/* (non-Javadoc)
	 * Method declared in IActionDelegate
	 */
	public void init(IWorkbenchWindow window) {
		super.init(window);
		StructuredSelectionProvider provider= StructuredSelectionProvider.createFrom(window.getSelectionService());
		initPossibleTargets(new RefactoringAction[] {
			new SelfEncapsulateFieldAction(provider)		
		});
	}
}

