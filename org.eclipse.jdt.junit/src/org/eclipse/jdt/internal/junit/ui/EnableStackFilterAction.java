/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.junit.ui;

import java.net.MalformedURLException;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;

/**
 * Action to enable/disable stack trace filtering.
 */
public class EnableStackFilterAction extends Action {

	private FailureTraceView fView;	
	
	public EnableStackFilterAction(FailureTraceView view) {
		super(JUnitMessages.getString("EnableStackFilterAction.action.label"));  //$NON-NLS-1$
		setDescription(JUnitMessages.getString("EnableStackFilterAction.action.description"));  //$NON-NLS-1$
		setToolTipText(JUnitMessages.getString("EnableStackFilterAction.action.tooltip")); //$NON-NLS-1$
		
		setImageDescriptors(); 
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
	
	private void setImageDescriptors() {	
		try {
			ImageDescriptor id= ImageDescriptor.createFromURL(JUnitPlugin.makeIconFileURL("dlfilter.gif"));  //$NON-NLS-1$
			if (id != null)
				setDisabledImageDescriptor(id);
				
			id= ImageDescriptor.createFromURL(JUnitPlugin.makeIconFileURL("clfilter.gif"));  //$NON-NLS-1$
			if (id != null)
				setHoverImageDescriptor(id);
	
			id= ImageDescriptor.createFromURL(JUnitPlugin.makeIconFileURL("elfilter.gif"));  //$NON-NLS-1$
			if (id != null)
				setImageDescriptor(id);
				
		} catch (MalformedURLException e) {
		}
	}
}