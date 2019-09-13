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
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation
 * 			(report 36180: Callers/Callees view)
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.callhierarchy;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jface.action.Action;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * Toggles the orientationof the layout of the call hierarchy
 */
class ToggleOrientationAction extends Action {

    private CallHierarchyViewPart fView;
    private int fActionOrientation;

    public ToggleOrientationAction(CallHierarchyViewPart v, int orientation) {
        super("", AS_RADIO_BUTTON); //$NON-NLS-1$
		switch (orientation) {
		case CallHierarchyViewPart.VIEW_ORIENTATION_HORIZONTAL:
			setText(CallHierarchyMessages.ToggleOrientationAction_horizontal_label);
			setDescription(CallHierarchyMessages.ToggleOrientationAction_horizontal_description);
			setToolTipText(CallHierarchyMessages.ToggleOrientationAction_horizontal_tooltip);
			JavaPluginImages.setLocalImageDescriptors(this, "th_horizontal.png"); //$NON-NLS-1$
			break;
		case CallHierarchyViewPart.VIEW_ORIENTATION_VERTICAL:
			setText(CallHierarchyMessages.ToggleOrientationAction_vertical_label);
			setDescription(CallHierarchyMessages.ToggleOrientationAction_vertical_description);
			setToolTipText(CallHierarchyMessages.ToggleOrientationAction_vertical_tooltip);
			JavaPluginImages.setLocalImageDescriptors(this, "th_vertical.png"); //$NON-NLS-1$
			break;
		case CallHierarchyViewPart.VIEW_ORIENTATION_AUTOMATIC:
			setText(CallHierarchyMessages.ToggleOrientationAction_automatic_label);
			setDescription(CallHierarchyMessages.ToggleOrientationAction_automatic_description);
			setToolTipText(CallHierarchyMessages.ToggleOrientationAction_automatic_tooltip);
			JavaPluginImages.setLocalImageDescriptors(this, "th_automatic.png"); //$NON-NLS-1$
			break;
		case CallHierarchyViewPart.VIEW_ORIENTATION_SINGLE:
			setText(CallHierarchyMessages.ToggleOrientationAction_single_label);
			setDescription(CallHierarchyMessages.ToggleOrientationAction_single_description);
			setToolTipText(CallHierarchyMessages.ToggleOrientationAction_single_tooltip);
			JavaPluginImages.setLocalImageDescriptors(this, "th_single.png"); //$NON-NLS-1$
			break;
		default:
			Assert.isTrue(false);
			break;
		}
        fView= v;
        fActionOrientation= orientation;
        PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.CALL_HIERARCHY_TOGGLE_ORIENTATION_ACTION);
    }

    public int getOrientation() {
        return fActionOrientation;
    }

    /*
     * @see Action#actionPerformed
     */
    @Override
	public void run() {
		if (isChecked()) {
			fView.fOrientation= fActionOrientation;
			fView.computeOrientation();
		}
    }

}
