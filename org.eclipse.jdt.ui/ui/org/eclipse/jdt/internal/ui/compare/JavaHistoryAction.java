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
import org.eclipse.ui.IActionDelegate;
import org.eclipse.jface.action.IAction;


/**
 * Base class for the "Replace with local history"
 * and "Add from local history" actions.
 */
public abstract class JavaHistoryAction extends Action
				implements ISelectionChangedListener, IUpdate, IActionDelegate { 
	
	private ISelectionProvider fSelectionProvider;
	private ISelection fSelection;

	
	public JavaHistoryAction() {			
	}
		
	public JavaHistoryAction(ISelectionProvider sp) {			
		fSelectionProvider= sp;
		Assert.isNotNull(fSelectionProvider);
	}
	
	ISelection getSelection() {
		if (fSelectionProvider != null)
			return fSelectionProvider.getSelection();
		return fSelection;
	}
		
	/* (non Java doc)
	 * @see IUpdate#update
	 */
	public void update() {
		setEnabled(isEnabled(getSelection()));
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
			IStructuredSelection ss= (IStructuredSelection) selection;
			if (ss.size() == 1) {
				Object o= ss.getFirstElement();
				if (o instanceof IMember) {
					IMember m= (IMember) o;
					if (!m.isBinary() && JavaStructureCreator.hasEdition(m))
						return m;
				}
			}
		}
		return null;
	}
			
	abstract protected boolean isEnabled(ISelection selection);
	
	public void run(IAction action) {
		run();
	}
	
	/**
	 * Notifies this action delegate that the selection in the workbench has changed.
	 * <p>
	 * Implementers can use this opportunity to change the availability of the
	 * action or to modify other presentation properties.
	 * </p>
	 *
	 * @param action the action proxy that handles presentation portion of the action
	 * @param selection the current selection in the workbench
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		fSelection= selection;
		action.setEnabled(isEnabled(selection));
	}
}
