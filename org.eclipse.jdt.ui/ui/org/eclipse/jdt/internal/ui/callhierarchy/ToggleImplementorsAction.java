/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation 
 * 			(report 36180: Callers/Callees view)
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.callhierarchy;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.util.Assert;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * Toggles the orientationof the layout of the call hierarchy
 */
class ToggleImplementorsAction extends Action {

    private CallHierarchyViewPart fView;    
    private int fImplementorsMode;
    
    public ToggleImplementorsAction(CallHierarchyViewPart v, int implementorsMode) {
        super("", AS_RADIO_BUTTON); //$NON-NLS-1$
        if (implementorsMode == CallHierarchyViewPart.IMPLEMENTORS_ENABLED) {
            setText(CallHierarchyMessages.getString("ToggleImplementorsAction.enabled.label")); //$NON-NLS-1$
            setDescription(CallHierarchyMessages.getString("ToggleImplementorsAction.enabled.description")); //$NON-NLS-1$
            setToolTipText(CallHierarchyMessages.getString("ToggleImplementorsAction.enabled.tooltip")); //$NON-NLS-1$
            JavaPluginImages.setLocalImageDescriptors(this, "th_horizontal.gif"); //$NON-NLS-1$
        } else if (implementorsMode == CallHierarchyViewPart.IMPLEMENTORS_DISABLED) {
            setText(CallHierarchyMessages.getString("ToggleImplementorsAction.disabled.label")); //$NON-NLS-1$
            setDescription(CallHierarchyMessages.getString("ToggleImplementorsAction.disabled.description")); //$NON-NLS-1$
            setToolTipText(CallHierarchyMessages.getString("ToggleImplementorsAction.disabled.tooltip")); //$NON-NLS-1$  
            JavaPluginImages.setLocalImageDescriptors(this, "th_vertical.gif"); //$NON-NLS-1$
        } else {
            Assert.isTrue(false);
        }
        fView= v;
        fImplementorsMode= implementorsMode;
        WorkbenchHelp.setHelp(this, IJavaHelpContextIds.CALL_HIERARCHY_TOGGLE_IMPLEMENTORS_ACTION);
    }
    
    public int getImplementorsMode() {
        return fImplementorsMode;
    }   
    
    /*
     * @see Action#actionPerformed
     */     
    public void run() {
        fView.setImplementorsMode(fImplementorsMode); // will toggle the checked state
    }
}
