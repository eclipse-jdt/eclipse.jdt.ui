/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.ui;


import org.eclipse.jface.action.Action;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.internal.junit.JUnitPreferencesConstants;

/**
 * Action to enable/disable stack trace filtering.
 */
public class EnableStackFilterAction extends Action {

	private FailureTrace fView;

	public EnableStackFilterAction(FailureTrace view) {
		super(JUnitMessages.EnableStackFilterAction_action_label);
		setDescription(JUnitMessages.EnableStackFilterAction_action_description);
		setToolTipText(JUnitMessages.EnableStackFilterAction_action_tooltip);

		setDisabledImageDescriptor(JUnitPlugin.getImageDescriptor("dlcl16/cfilter.png")); //$NON-NLS-1$
		setHoverImageDescriptor(JUnitPlugin.getImageDescriptor("elcl16/cfilter.png")); //$NON-NLS-1$
		setImageDescriptor(JUnitPlugin.getImageDescriptor("elcl16/cfilter.png")); //$NON-NLS-1$
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJUnitHelpContextIds.ENABLEFILTER_ACTION);

		fView= view;
		setChecked(JUnitPreferencesConstants.getFilterStack());
	}

	/*
	 * @see Action#actionPerformed
	 */
	@Override
	public void run() {
		JUnitPreferencesConstants.setFilterStack(isChecked());
		fView.refresh();
	}
}
