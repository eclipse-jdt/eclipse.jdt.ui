/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.debug.ui;

import java.util.Iterator;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.*;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.internal.ui.BreakpointsView;
import org.eclipse.jdt.debug.core.IJavaExceptionBreakpoint;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

/**
 * Superclass for actions that toggle the caught/uncaught state of an exception breakpoint 
 */
public abstract class ExceptionAction extends Action implements IViewActionDelegate, IBreakpointListener {

	private IAction fAction= null;
	protected IStructuredSelection fCurrentSelection;

	public ExceptionAction() {
		setEnabled(false);
	}

	/**
	 * @see IActionDelegate
	 */
	public void run(IAction action) {
		fAction= action;
		IStructuredSelection selection= getStructuredSelection();
		Iterator enum= selection.iterator();
		while (enum.hasNext()) {
			try {
				IJavaExceptionBreakpoint breakpoint= (IJavaExceptionBreakpoint)enum.next();
				doAction(breakpoint);
			} catch (CoreException e) {
				DebugUIUtils.errorDialog(JavaPlugin.getActiveWorkbenchShell(),"exception_action.error.", e.getStatus());
			}
		}
	}

	/**
	 * @see IAction
	 */
	public void run() {
		run(null);
	}

	/**
	 * @see IActionDelegate
	 */
	public void selectionChanged(IAction action, ISelection sel) {
		fAction= action;
		if (sel instanceof IStructuredSelection) {
			fCurrentSelection= (IStructuredSelection)sel;
			boolean enabled= fCurrentSelection.size() == 1 && isEnabledFor(fCurrentSelection.getFirstElement());
			action.setEnabled(enabled);
			if (enabled) {
				IBreakpoint breakpoint= (IBreakpoint)fCurrentSelection.getFirstElement();
				if (breakpoint instanceof IJavaExceptionBreakpoint) {
					try {
						action.setChecked(getToggleState((IJavaExceptionBreakpoint) breakpoint));
					} catch (CoreException e) {
					}
				}
			}
		}
	}

	/**
	 * Toggle the state of this action
	 */
	public abstract void doAction(IJavaExceptionBreakpoint exception) throws CoreException;

	/**
	 * Returns whether this action is currently toggled on
	 */
	protected abstract boolean getToggleState(IJavaExceptionBreakpoint exception) throws CoreException;

	/**
	 * @see IViewActionDelegate
	 */
	public void init(IViewPart viewPart) {
		if (viewPart instanceof BreakpointsView) {
			((BreakpointsView)viewPart).addBreakpointListenerAction(this);
		}		
	}

	protected IStructuredSelection getStructuredSelection() {
		return fCurrentSelection;
	}

	public boolean isEnabledFor(Object element) {
		return element instanceof IJavaExceptionBreakpoint;
	}
	
	/** 
	 * @see IBreakpointListener
	 */
	public void breakpointAdded(IBreakpoint breakpoint) {
	}

	/** 
	 * @see IBreakpointListener
	 */
	public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
	}

	/** 
	 * @see IBreakpointListener
	 */
	public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
		final Display display= Display.getDefault();
		if (display.isDisposed()) {
			return;
		}
		display.asyncExec(new Runnable() {
			public void run() {
				if (fAction != null && fCurrentSelection != null) {
					selectionChanged(fAction, fCurrentSelection);
				}
			}
		});
	}		
	
	/**
	 * Returns the breakpoint manager for this plugin
	 */
	private IBreakpointManager getBreakpointManager() {
		return DebugPlugin.getDefault().getBreakpointManager();
	}
	
	/**
	 * Returns the breakpoint associated with the given marker
	 */
	private IBreakpoint getBreakpoint(IMarker marker) {
		return getBreakpointManager().getBreakpoint(marker);
	}	

}
