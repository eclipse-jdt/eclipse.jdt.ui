/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.reorg;

import java.util.List;

import org.eclipse.jdt.internal.ui.refactoring.actions.StructuredSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
public class RenameAction extends ReorgAction {
	
	private IRefactoringRenameSupport fRefactoringSupport;
	
	public RenameAction(StructuredSelectionProvider provider) {
		super(ReorgMessages.getString("renameAction.label"), provider); //$NON-NLS-1$
		setDescription(ReorgMessages.getString("renameAction.description")); //$NON-NLS-1$
	}

	/**
	 *The user has invoked this action
	 */
	public void run() {
		fRefactoringSupport.rename(getStructuredSelection().getFirstElement());
	}
	
	/* non java-doc
	 * @see IRefactoringAction#canOperateOn(IStructuredSelection)
	 */
	public boolean canOperateOn(IStructuredSelection selection) {
		if (selection.size() != 1)
			return false;

		Object element= selection.getFirstElement();
		fRefactoringSupport= RefactoringSupportFactory.createRenameSupport(element);
		if (fRefactoringSupport == null)
			return false;
		return fRefactoringSupport.canRename(element);
	}
		
	String getActionName() {
		return ReorgMessages.getString("renameAction.name"); //$NON-NLS-1$
	}	
}
