package org.eclipse.jdt.internal.debug.ui;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import java.util.Iterator;
/**
 * Toggles the caught/uncaught state of an exception breakpoint 
 */
public abstract class ExceptionAction extends Action implements IViewActionDelegate {

	protected IStructuredSelection fCurrentSelection;
	/**
	 * Creates the action to set the hit count for breakpoints
	 */
	public ExceptionAction() {
		setEnabled(false);
	}

	/**
	 * @see IActionDelegate
	 */
	public void run(IAction action) {
		IStructuredSelection selection= getStructuredSelection();
		//Get the selected marker
		Iterator enum= selection.iterator();
		while (enum.hasNext()) {
			doAction((IMarker) enum.next());
		}
	}

	/**
	 * @see IAction
	 */
	public void run() {
		run(null);
	}

	public abstract void doAction(IMarker exception);

	/**
	 * @see IActionDelegate
	 */
	public void selectionChanged(IAction action, ISelection sel) {
		if (sel instanceof IStructuredSelection) {
			fCurrentSelection= (IStructuredSelection) sel;
			boolean enabled= fCurrentSelection.size() == 1 && isEnabledFor(fCurrentSelection.getFirstElement());
			action.setEnabled(enabled);
			if (enabled) {
				action.setChecked(getToggleState((IMarker) fCurrentSelection.getFirstElement()));
			}
		}
	}

	/**
	 * Returns
	 */
	protected abstract boolean getToggleState(IMarker exception);

	/**
	 * @see IViewActionDelegate
	 */
	public void init(IViewPart view) {
	}

	protected IStructuredSelection getStructuredSelection() {
		return fCurrentSelection;
	}

	public boolean isEnabledFor(Object element) {
		return element instanceof IMarker && JDIDebugModel.isExceptionBreakpoint((IMarker) element);
	}

}
