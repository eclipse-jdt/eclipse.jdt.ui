/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.junit.ui;


import org.eclipse.jface.action.Action;
import org.eclipse.ui.help.WorkbenchHelp;

/**
 * Action to enable/disable stack trace filtering.
 */
public class EnableStackFilterAction extends Action {

	private FailureTraceView fView;	
	
	public EnableStackFilterAction(FailureTraceView view) {
		super(JUnitMessages.getString("EnableStackFilterAction.action.label"));  //$NON-NLS-1$
		setDescription(JUnitMessages.getString("EnableStackFilterAction.action.description"));  //$NON-NLS-1$
		setToolTipText(JUnitMessages.getString("EnableStackFilterAction.action.tooltip")); //$NON-NLS-1$
		
		setDisabledImageDescriptor(JUnitPlugin.getImageDescriptor("dlcl16/cfilter.gif")); //$NON-NLS-1$
		setHoverImageDescriptor(JUnitPlugin.getImageDescriptor("clcl16/cfilter.gif")); //$NON-NLS-1$
		setImageDescriptor(JUnitPlugin.getImageDescriptor("elcl16/cfilter.gif")); //$NON-NLS-1$
		WorkbenchHelp.setHelp(this, IJUnitHelpContextIds.ENABLEFILTER_ACTION);

		fView= view;
		setChecked(JUnitPreferencePage.getFilterStack());
	}

	/*
	 * @see Action#actionPerformed
	 */		
	public void run() {
		JUnitPreferencePage.setFilterStack(isChecked());
		fView.refresh();
	}
}