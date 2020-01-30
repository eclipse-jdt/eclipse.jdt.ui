/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.callhierarchy;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPluginImages;


/**
 * The action to pin the Call Hierarchy view.
 *
 * @since 3.7
 */
class PinCallHierarchyViewAction extends Action {
	private CallHierarchyViewPart fView= null;

	/**
	 * Constructs a 'Pin Call Hierarchy view' action.
	 *
	 * @param view the Call Hierarchy view
	 */
	public PinCallHierarchyViewAction(CallHierarchyViewPart view) {
		super(CallHierarchyMessages.PinCallHierarchyViewAction_label, IAction.AS_CHECK_BOX);
		setToolTipText(CallHierarchyMessages.PinCallHierarchyViewAction_tooltip);
		JavaPluginImages.setLocalImageDescriptors(this, "pin_view.png"); //$NON-NLS-1$
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.CALL_HIERARCHY_PIN_VIEW_ACTION);
		fView= view;
	}

	/*
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public void run() {
		fView.setPinned(isChecked());
	}
}
