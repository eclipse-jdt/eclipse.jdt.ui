package org.eclipse.jdt.internal.debug.ui;

import java.util.Iterator;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.*;
import org.eclipse.debug.internal.ui.BreakpointsView;
import org.eclipse.jdt.debug.core.IJavaWatchpoint;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

public abstract class WatchpointAction extends Action implements IViewActionDelegate, IBreakpointListener {
	
	private IStructuredSelection fCurrentSelection;
	private IAction fAction;
	
	public WatchpointAction() {
		setEnabled(false);
	}

	/**
	 * @see Action#run()
	 */
	public void run() {
		run(null);
	}

	/**
	 * @see IViewActionDelegate#init(IViewPart)
	 */
	public void init(IViewPart viewPart) {
		if (viewPart instanceof BreakpointsView) {
			((BreakpointsView)viewPart).addBreakpointListenerAction(this);
		}
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		fAction= action;
		IStructuredSelection selection= getStructuredSelection();
		Iterator enum= selection.iterator();
		while (enum.hasNext()) {
			try {
				IBreakpoint breakpoint= getBreakpoint((IMarker) enum.next());
				if (breakpoint instanceof IJavaWatchpoint) {
					doAction((IJavaWatchpoint)breakpoint);
				}
			} catch (CoreException e) {
				DebugUIUtils.errorDialog(JavaPlugin.getActiveWorkbenchShell(),"watchpoint_action.error", e.getStatus());
			}			
		}
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		if (selection.isEmpty()) {
			fCurrentSelection= null;
		}
		else if (selection instanceof IStructuredSelection) {
			fCurrentSelection= (IStructuredSelection)selection;
			boolean enabled= fCurrentSelection.size() == 1 && isEnabledFor(fCurrentSelection.getFirstElement());
			action.setEnabled(enabled);
			if (enabled) {
				IBreakpoint breakpoint= getBreakpoint((IMarker)fCurrentSelection.getFirstElement());
				if (breakpoint instanceof IJavaWatchpoint) {
					action.setChecked(getToggleState((IJavaWatchpoint) breakpoint));
				}
			}
		}
	}

	/**
	 * Toggle the state of this action
	 */
	public abstract void doAction(IJavaWatchpoint watchpoint) throws CoreException;
	
	/**
	 * Returns whether this action is currently toggled on
	 */
	protected abstract boolean getToggleState(IJavaWatchpoint watchpoint);
	
	/**
	 * Get the current selection
	 */
	protected IStructuredSelection getStructuredSelection() {
		return fCurrentSelection;
	}
	
	public boolean isEnabledFor(Object element) {
		if (element instanceof IMarker) {
			IBreakpoint breakpoint= getBreakpoint((IMarker) element);
			return breakpoint instanceof IJavaWatchpoint;
		}
		return false;
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
	 * Get the breakpoint manager for the debug plugin
	 */
	protected IBreakpointManager getBreakpointManager() {
		return DebugPlugin.getDefault().getBreakpointManager();		
	}
	
	/**
	 * Get the breakpoint associated with the given marker
	 */
	protected IBreakpoint getBreakpoint(IMarker marker) {
		return getBreakpointManager().getBreakpoint(marker);
	}

}

