/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.ui.IWorkbenchWindow;

import org.eclipse.jdt.internal.ui.actions.StructuredSelectionProvider;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.reorg.RenameAction;

public class RenameActionDelegate extends RefactoringActionDelegate {

	public RenameActionDelegate() {
		super(RefactoringMessages.getString("RenameActionDelegate.rename"), //$NON-NLS-1$
			RefactoringMessages.getString("RenameActionDelegate.unavailable")); //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * Method declared in IActionDelegate
	 */
	public void init(IWorkbenchWindow window) {
		super.init(window);
		
		RenameTempAction rta= new RenameTempAction();
		rta.init(window);
		
		StructuredSelectionProvider provider= StructuredSelectionProvider.createFrom(window.getSelectionService());
		initPossibleTargets(new IRefactoringAction[] {
			//the sequence is important here (see bug 12590)
			rta,
			new RenameAction(provider)
		});
	}
}

