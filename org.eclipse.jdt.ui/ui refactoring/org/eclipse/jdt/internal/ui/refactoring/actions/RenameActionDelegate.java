/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.ui.IWorkbenchWindow;

import org.eclipse.jdt.internal.ui.actions.StructuredSelectionProvider;
import org.eclipse.jdt.internal.ui.reorg.RenameAction;

public class RenameActionDelegate extends RefactoringActionDelegate {

	public RenameActionDelegate() {
		super(RefactoringMessages.getString("RenameActionDelegate.rename"), "Operation unavailable on the current selection.\n" //$NON-NLS-1$
		       + "Select a java project, source folder, resource, package, compilation unit, type, field, method, parameter or a local variable.");
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

