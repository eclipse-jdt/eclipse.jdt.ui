/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.compare;

import java.text.MessageFormat;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.*;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;

import org.eclipse.ui.texteditor.IUpdate;


public abstract class JavaHistoryAction extends Action implements ISelectionChangedListener, IUpdate {
	
	protected ISelectionProvider fSelectionProvider;

	
	public JavaHistoryAction(ISelectionProvider sp) {			
		fSelectionProvider= sp;
	}
		
	/**
	 * @see IUpdate#update
	 */
	public void update() {
		updateLabel(fSelectionProvider.getSelection());
	}
	
	/**
	 * @see ISelectionAction#selectionChanged
	 */	
	public final void selectionChanged(SelectionChangedEvent e) {
		updateLabel(e.getSelection());
	}
		
	IMember getEditionElement(ISelection selection) {
		
		if (selection instanceof IStructuredSelection) {
			Object[] o= ((IStructuredSelection)selection).toArray();
			if (o != null && o.length == 1 && o[0] instanceof IMember) {
				IMember m= (IMember) o[0];
				if (!m.isBinary() && JavaStructureCreator.hasEdition((IJavaElement) o[0]))
					return m;
			}
		}
		return null;
	}
	
	void updateLabel(ISelection selection) {
		setEnabled(getLabelName(selection) != null);
	}
	
	protected String getLabelName(ISelection selection) {
		return null;
	}
}