/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.actions;

import org.eclipse.jface.action.Action;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPluginImages;


/**
 * This is an action template for actions that toggle whether
 * it links its selection to the active editor.
 *
 * @since 3.0
 */
public abstract class AbstractToggleLinkingAction extends Action {

	/**
	 * Constructs a new action.
	 */
	public AbstractToggleLinkingAction() {
		super(ActionMessages.ToggleLinkingAction_label);
		setDescription(ActionMessages.ToggleLinkingAction_description);
		setToolTipText(ActionMessages.ToggleLinkingAction_tooltip);
		JavaPluginImages.setLocalImageDescriptors(this, "synced.png"); //$NON-NLS-1$
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.LINK_EDITOR_ACTION);
		setChecked(false);
	}

	/**
	 * Runs the action.
	 */
	@Override
	public abstract void run();
}
