package org.eclipse.jdt.internal.debug.ui;

import java.util.Iterator;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.internal.ui.BreakpointsView;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.internal.debug.core.DebugJavaUtils;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.texteditor.IUpdate;

public abstract class WatchpointAction extends Action implements IViewActionDelegate, IUpdate {
	
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
			((BreakpointsView) viewPart).addContributedAction(this);
		}
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		IStructuredSelection selection= getStructuredSelection();
		Iterator enum= selection.iterator();
		while (enum.hasNext()) {
			try {
				IMarker watchpoint= (IMarker) enum.next();
				doAction(watchpoint);
				fixUpState(watchpoint);
			} catch (CoreException e) {
				DebugUIUtils.errorDialog(JavaPlugin.getActiveWorkbenchShell(),"watchpoint_action.error", e.getStatus());
			}			
		}
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			fAction= action;			
			fCurrentSelection= (IStructuredSelection) selection;
			update();
		}
	}

	/**
	 * Toggle the state of this action
	 */
	public abstract void doAction(IMarker watchpoint) throws CoreException;
	
	/**
	 * Returns whether this action is currently toggled on
	 */
	protected abstract boolean getToggleState(IMarker watchpoint);
	
	/**
	 * Get the current selection
	 */
	protected IStructuredSelection getStructuredSelection() {
		return fCurrentSelection;
	}
	
	public boolean isEnabledFor(Object element) {
		return element instanceof IMarker && JDIDebugModel.isWatchpoint((IMarker)element);
	}

	/**
	 * Disable the given watchpoint.
	 */
	public void disableWatchpoint(IMarker watchpoint) {
		try {
			getBreakpointManager().setEnabled(watchpoint, false);
			DebugJavaUtils.setAutoDisabled(watchpoint, true);
		} catch (CoreException ce) {
			DebugJavaUtils.logError(ce);
		}
	}

	/**
	 * Enable the given watchpoint.
	 */
	public void enableWatchpoint(IMarker watchpoint) {
		try {
			IBreakpointManager manager= getBreakpointManager();
			if (!manager.isEnabled(watchpoint)) {
				manager.setEnabled(watchpoint, true);
				DebugJavaUtils.setAutoDisabled(watchpoint, false);				
			}
		} catch (CoreException ce) {
			DebugJavaUtils.logError(ce);
		}
	}
	
	/**
	 * Get the breakpoint manager for the debug plugin
	 */
	private IBreakpointManager getBreakpointManager() {
		return DebugPlugin.getDefault().getBreakpointManager();		
	}

	/**
	 * @see IUpdate#update()
	 */
	public void update() {
		if (fAction == null) {
			return;
		}
		boolean enabled= fCurrentSelection.size() == 1 && isEnabledFor(fCurrentSelection.getFirstElement());
		fAction.setEnabled(enabled);
		if (enabled) {
			IMarker watchpoint= (IMarker)fCurrentSelection.getFirstElement();
			if (getBreakpointManager().isEnabled(watchpoint)) {
				try {
					fixUpState(watchpoint);					
					DebugJavaUtils.setAutoDisabled(watchpoint, false);
				} catch (CoreException ce) {
					DebugJavaUtils.logError(ce);					
				}
			}
			fAction.setChecked(getToggleState(watchpoint));
		}		
	}

	/**
	 * Fix up the state of the breakpoint such that the enabled/disabled state makes
	 * sense in relation to the access and modification settings.
	 * The goal is to ensure that a watchpoint is never displayed as enabled
	 * with neither access or modification on.
	 * If access and modification are both deselected, disable the breakpoint.
	 * If access and modification are both deselected and the user chooses to
	 * enable the breakpoint, set access and modification to their default value
	 * (this assumes that the default value is such that ((access || modification) == true).
	 * If the breakpoint is disabled and the user turns on access or modification,
	 * enable the breakpoint.
	 */
	private void fixUpState(IMarker watchpoint) {
		boolean access, modification, enabled, auto_disabled;
		access= DebugJavaUtils.isAccess(watchpoint);
		modification= DebugJavaUtils.isModification(watchpoint);
		enabled= getBreakpointManager().isEnabled(watchpoint);
		auto_disabled= DebugJavaUtils.isAutoDisabled(watchpoint);
		if (!(access || modification) && enabled && auto_disabled) {
			try {
				DebugJavaUtils.setDefaultAccessAndModification(watchpoint);
				enableWatchpoint(watchpoint);
			} catch (CoreException ce) {
				DebugJavaUtils.logError(ce);
			}
		} else if (!(access || modification) && enabled) {
			disableWatchpoint(watchpoint);
		} else {
	        enableWatchpoint(watchpoint);
		}		
	}

}

