/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jdt.internal.ui.actions.StructuredSelectionProvider;

public abstract class RefactoringAction extends Action implements IRefactoringAction {
	
	private StructuredSelectionProvider fProvider;
	
	protected RefactoringAction(String text, ISelectionProvider selectionProvider){
		this(text, StructuredSelectionProvider.createFrom(selectionProvider));
	}
	
	protected RefactoringAction(String text, StructuredSelectionProvider provider) {
		super(text);
		Assert.isNotNull(provider);
		fProvider= provider;
	}
	
	protected StructuredSelectionProvider getProvider() {
		return fProvider;
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
		if (selection == null)
			setEnabled(false);
		else	
			setEnabled(canOperateOn(selection));
	}
}