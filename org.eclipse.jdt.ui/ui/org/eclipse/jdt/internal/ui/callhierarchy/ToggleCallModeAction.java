/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * Toggles the call direction of the call hierarchy (i.e. toggles between showing callers and callees.)
 */
class ToggleCallModeAction extends Action {

    private CallHierarchyViewPart fView;    
    private int fMode;
    
    public ToggleCallModeAction(CallHierarchyViewPart v, int mode) {
        super("", AS_RADIO_BUTTON); //$NON-NLS-1$
        if (mode == CallHierarchyViewPart.CALL_MODE_CALLERS) {
            setText(CallHierarchyMessages.getString("ToggleCallModeAction.callers.label")); //$NON-NLS-1$
            setDescription(CallHierarchyMessages.getString("ToggleCallModeAction.callers.description")); //$NON-NLS-1$
            setToolTipText(CallHierarchyMessages.getString("ToggleCallModeAction.callers.tooltip")); //$NON-NLS-1$
            JavaPluginImages.setLocalImageDescriptors(this, "ch_callers.gif"); //$NON-NLS-1$
        } else if (mode == CallHierarchyViewPart.CALL_MODE_CALLEES) {
            setText(CallHierarchyMessages.getString("ToggleCallModeAction.callees.label")); //$NON-NLS-1$
            setDescription(CallHierarchyMessages.getString("ToggleCallModeAction.callees.description")); //$NON-NLS-1$
            setToolTipText(CallHierarchyMessages.getString("ToggleCallModeAction.callees.tooltip")); //$NON-NLS-1$  
            JavaPluginImages.setLocalImageDescriptors(this, "ch_callees.gif"); //$NON-NLS-1$
        } else {
            Assert.isTrue(false);
        }
        fView= v;
        fMode= mode;
        PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.CALL_HIERARCHY_TOGGLE_CALL_MODE_ACTION);
    }
    
    public int getMode() {
        return fMode;
    }   
    
    /*
     * @see Action#actionPerformed
     */     
    public void run() {
        fView.setCallMode(fMode); // will toggle the checked state
    }
    
}
