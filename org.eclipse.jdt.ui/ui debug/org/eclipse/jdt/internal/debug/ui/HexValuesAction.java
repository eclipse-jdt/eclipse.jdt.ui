package org.eclipse.jdt.internal.debug.ui;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2001
 */
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IDebugViewAdapter;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

/**
 * An action that toggles the state of its viewer to
 * show/hide hexidecimal values for primitive variables.
 */

public class HexValuesAction extends Action implements IViewActionDelegate {

	private static final String PREFIX= "hex_values_action.";
	private static final String SHOW= PREFIX + "show";
	private static final String HIDE= PREFIX + "hide";

	IDebugViewAdapter fAdapter;

	public HexValuesAction() {
		super(DebugUIUtils.getResourceString(SHOW));
		setToolTipText(DebugUIUtils.getResourceString(SHOW));
	}

	public void run(IAction action) {
		valueChanged(action.isChecked());
		String label= action.isChecked() ? DebugUIUtils.getResourceString(HIDE) : DebugUIUtils.getResourceString(SHOW);
		action.setToolTipText(label);
		action.setText(label);
	}

	/**
	 * @see IAction
	 */
	public void run() {
	}

	private void valueChanged(boolean on) {
		IDebugModelPresentation presentation= fAdapter.getPresentation(JDIDebugModel.getPluginIdentifier());
		presentation.setAttribute(JDIModelPresentation.DISPLAY_HEX_VALUES, new Boolean(on));
		fAdapter.getViewer().refresh();
		setToolTipText(on ? DebugUIUtils.getResourceString(HIDE) : DebugUIUtils.getResourceString(SHOW));
	}

	public void setChecked(boolean value) {
		super.setChecked(value);
		valueChanged(value);
	}

	/**
	 * @see IViewActionDelegate
	 */
	public void init(IViewPart view) {
		fAdapter= (IDebugViewAdapter) view.getAdapter(IDebugViewAdapter.class);
	}

	/**
	 * @see IViewActionDelegate
	 */
	public void selectionChanged(IAction action, ISelection selection) {
	}

}
