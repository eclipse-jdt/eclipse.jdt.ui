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
package org.eclipse.jdt.internal.ui.workingsets;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.util.Assert;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;

/**
 * Clears the selected working set in the action group's view.
 * 
 * @since 2.0
 */
public class ClearWorkingSetAction extends Action {
	
	private WorkingSetFilterActionGroup fActionGroup;

	public ClearWorkingSetAction(WorkingSetFilterActionGroup actionGroup) {
		super(WorkingSetMessages.getString("ClearWorkingSetAction.text")); //$NON-NLS-1$
		Assert.isNotNull(actionGroup);
		setToolTipText(WorkingSetMessages.getString("ClearWorkingSetAction.toolTip")); //$NON-NLS-1$
		setEnabled(actionGroup.getWorkingSet() != null);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.CLEAR_WORKING_SET_ACTION);
		fActionGroup= actionGroup;
	}

	/*
	 * Overrides method from Action
	 */
	public void run() {
		fActionGroup.setWorkingSet(null, true);
	}
}
