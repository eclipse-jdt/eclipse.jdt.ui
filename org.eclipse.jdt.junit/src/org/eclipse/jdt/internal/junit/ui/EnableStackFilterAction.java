/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.ui;


import org.eclipse.jface.action.Action;
import org.eclipse.ui.PlatformUI;

/**
 * Action to enable/disable stack trace filtering.
 */
public class EnableStackFilterAction extends Action {

	private FailureTrace fView;	
	
	public EnableStackFilterAction(FailureTrace view) {
		super(JUnitMessages.getString("EnableStackFilterAction.action.label"));  //$NON-NLS-1$
		setDescription(JUnitMessages.getString("EnableStackFilterAction.action.description"));  //$NON-NLS-1$
		setToolTipText(JUnitMessages.getString("EnableStackFilterAction.action.tooltip")); //$NON-NLS-1$
		
		setDisabledImageDescriptor(JUnitPlugin.getImageDescriptor("dlcl16/cfilter.gif")); //$NON-NLS-1$
		setHoverImageDescriptor(JUnitPlugin.getImageDescriptor("elcl16/cfilter.gif")); //$NON-NLS-1$
		setImageDescriptor(JUnitPlugin.getImageDescriptor("elcl16/cfilter.gif")); //$NON-NLS-1$
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJUnitHelpContextIds.ENABLEFILTER_ACTION);

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
