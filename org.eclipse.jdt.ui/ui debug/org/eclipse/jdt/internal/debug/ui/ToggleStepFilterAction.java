package org.eclipse.jdt.internal.debug.ui;

import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

/**
 * Toggles the global preference flag that controls whether the active step filters
 * defined in the Java Debug Options preference page are used.
 */
public class ToggleStepFilterAction extends Action implements IViewActionDelegate {

	// See comment in selectionChanged()
	private boolean fSetInitialState = false;

	/**
	 * @see IViewActionDelegate#init(IViewPart)
	 */
	public void init(IViewPart view) {		
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		boolean newStepFilterState = !JDIDebugModel.useStepFilters();		
		action.setChecked(newStepFilterState);
		JDIDebugModel.setUseStepFilters(newStepFilterState);
	}

	/**
	 * @see Action#run()
	 */
	public void run() {
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		// This is the only way to set the initial checked state of the action
		// See [1GJUUTP: ITPDUI:ALL - Cheesy code in ToggleStepFilterAction]
		if (!fSetInitialState) {
			action.setChecked(JDIDebugModel.useStepFilters());
			fSetInitialState = true;
		}
	}

}

