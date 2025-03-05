/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Stephan Herrmann (stephan@cs.tu-berlin.de):
 *          - bug 206949: [call hierarchy] filter field accesses (only write or only read)
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.callhierarchy;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jface.action.Action;

import org.eclipse.jdt.core.search.IJavaSearchConstants;

/**
 * Toggles how fields should be expanded: all references, read accesses, write accesses.
 */
public class SelectFieldModeAction extends Action {

    private final CallHierarchyViewPart fView;
    private final int fMode;

    public SelectFieldModeAction(CallHierarchyViewPart v, int mode) {
		super(null, AS_RADIO_BUTTON);
		switch (mode) {
			case IJavaSearchConstants.REFERENCES:
				setText(CallHierarchyMessages.SelectFieldModeAction_all_references_label);
				setDescription(CallHierarchyMessages.SelectFieldModeAction_all_references_description);
				break;
			case IJavaSearchConstants.READ_ACCESSES:
				setText(CallHierarchyMessages.SelectFieldModeAction_read_accesses_label);
				setDescription(CallHierarchyMessages.SelectFieldModeAction_read_accesses_description);
				break;
			case IJavaSearchConstants.WRITE_ACCESSES:
				setText(CallHierarchyMessages.SelectFieldModeAction_write_accesses_label);
				setDescription(CallHierarchyMessages.SelectFieldModeAction_write_accesses_description);
				break;
			default:
				Assert.isTrue(false);
				break;
		}
        fView= v;
        fMode= mode;
        // FIXME(stephan) adjust/create new help context
//        PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.CALL_HIERARCHY_TOGGLE_CALL_MODE_ACTION);
    }

    public int getMode() {
        return fMode;
    }

    /*
     * @see Action#actionPerformed
     */
    @Override
	public void run() {
        fView.setFieldMode(fMode); // will toggle the checked state
    }
}
