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
package org.eclipse.jdt.internal.ui.infoviews;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.util.Assert;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.ui.actions.OpenAction;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

class GotoInputAction extends Action {
	
	private AbstractInfoView fInfoView;

	public GotoInputAction(AbstractInfoView infoView) {
		Assert.isNotNull(infoView);
		fInfoView= infoView;

		JavaPluginImages.setLocalImageDescriptors(this, "goto_input.gif"); //$NON-NLS-1$
		setText(InfoViewMessages.getString("GotoInputAction.label")); //$NON-NLS-1$
		setToolTipText(InfoViewMessages.getString("GotoInputAction.tooltip")); //$NON-NLS-1$
		setDescription(InfoViewMessages.getString("GotoInputAction.description")); //$NON-NLS-1$

		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.OPEN_INPUT_ACTION);
	}
	
	public void run() {
		IJavaElement inputElement= fInfoView.getInput();
		new OpenAction(fInfoView.getViewSite()).run(new Object[] { inputElement });
	}
}
