/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.compare;

import java.text.MessageFormat;

import org.eclipse.jface.util.Assert;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.*;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;

import org.eclipse.ui.texteditor.IUpdate;

/**
 * Base class for the "Replace with local history"
 * and "Add from local history" actions.
 */
public abstract class JavaHistoryAction extends Action implements ISelectionChangedListener, IUpdate {
	
	protected ISelectionProvider fSelectionProvider;

	
	public JavaHistoryAction(ISelectionProvider sp) {			
		fSelectionProvider= sp;
		Assert.isNotNull(fSelectionProvider);
	}
		
	/* (non Java doc)
	 * @see IUpdate#update
	 */
	public void update() {
		setEnabled(isEnabled(fSelectionProvider.getSelection()));
	}
	
	/* (non Java doc)
	 * @see ISelectionAction#selectionChanged
	 */	
	public final void selectionChanged(SelectionChangedEvent e) {
		setEnabled(isEnabled(e.getSelection()));
	}
		
	/**
	 * Returns an IMember or null.
	 */
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
	
	abstract protected boolean isEnabled(ISelection selection);
}
