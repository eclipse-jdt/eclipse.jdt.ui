/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.ISelectionService;

public abstract class RefactoringAction extends Action {

	private StructuredSelectionProvider fProvider;

	protected RefactoringAction(String text, StructuredSelectionProvider provider) {
		super(text);
		fProvider= provider;
		Assert.isNotNull(fProvider);
	}
	
	/**
	 * Returns <code>true</code> iff the action can operate on the specified selection.
	 * @return <code>true</code> if the action can operate on the specified selection, 
	 *  <code>false</code> otherwise.
	 */
	public abstract boolean canOperateOn(IStructuredSelection selection);
	
	/**
	 * Returns the current selection.
	 * 
	 * @return the current selection as a structured selection or <code>null</code>
	 */
	protected IStructuredSelection getStructuredSelection() {
		return fProvider.getSelection();
	}
	
	/**
	 * Update the action's enable state according to the current selection of
	 * the used selection provider.
	 */
	public void update() {
		IStructuredSelection selection= getStructuredSelection();
		boolean enabled= false;
		if (selection != null)
			enabled= canOperateOn(selection);
			
		setEnabled(enabled);
	}	
}

