/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.reorg;

import java.util.List;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
public class RenameAction extends ReorgAction {
	
	private IRefactoringRenameSupport fRefactoringSupport;
	
	public RenameAction(ISelectionProvider viewer) {
		super(viewer, ReorgMessages.getString("renameAction.label")); //$NON-NLS-1$
		setDescription(ReorgMessages.getString("renameAction.description")); //$NON-NLS-1$
	}

	public void update() {
		List sel= ((IStructuredSelection)getSelectionProvider().getSelection()).toList();
		setEnabled(canExecute(sel));
	}
	
	/**
	 *The user has invoked this action
	 */
	public void run() {
		fRefactoringSupport.rename(getStructuredSelection().getFirstElement());
	}
		
	boolean canExecute(List selection) {
		if (selection.size() != 1)
			return false;

		Object element= selection.get(0);
		fRefactoringSupport= RefactoringSupportFactory.createRenameSupport(element);
		if (fRefactoringSupport == null)
			return false;
		return fRefactoringSupport.canRename(element);
	}
		
	String getActionName() {
		return ReorgMessages.getString("renameAction.name"); //$NON-NLS-1$
	}	
}
