/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.ui.IWorkbenchWindow;

import org.eclipse.jdt.internal.ui.actions.StructuredSelectionProvider;

public class ModifyParametersActionDelegate extends RefactoringActionDelegate {

	public ModifyParametersActionDelegate() {
		super(RefactoringMessages.getString("ModifyParametersActionDelegate.modify_parameters"), RefactoringMessages.getString("ModifyParametersActionDelegate.unavailable")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/* (non-Javadoc)
	 * Method declared in IActionDelegate
	 */
	public void init(IWorkbenchWindow window) {
		super.init(window);
		StructuredSelectionProvider provider= StructuredSelectionProvider.createFrom(window.getSelectionService());
		initPossibleTargets(new RefactoringAction[] {
			RefactoringGroup.createModifyParametersAction(provider)
		});
	}
}

