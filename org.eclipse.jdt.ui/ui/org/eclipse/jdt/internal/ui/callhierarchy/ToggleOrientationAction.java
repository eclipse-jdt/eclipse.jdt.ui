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
class ToggleOrientationAction extends Action {

    private CallHierarchyViewPart fView;    
    private int fOrientation;
    
    public ToggleOrientationAction(CallHierarchyViewPart v, int orientation) {
        super("", AS_RADIO_BUTTON); //$NON-NLS-1$
        if (orientation == CallHierarchyViewPart.VIEW_ORIENTATION_HORIZONTAL) {
            setText(CallHierarchyMessages.getString("ToggleOrientationAction.horizontal.label")); //$NON-NLS-1$
            setDescription(CallHierarchyMessages.getString("ToggleOrientationAction.horizontal.description")); //$NON-NLS-1$
            setToolTipText(CallHierarchyMessages.getString("ToggleOrientationAction.horizontal.tooltip")); //$NON-NLS-1$
            JavaPluginImages.setLocalImageDescriptors(this, "th_horizontal.gif"); //$NON-NLS-1$
        } else if (orientation == CallHierarchyViewPart.VIEW_ORIENTATION_VERTICAL) {
            setText(CallHierarchyMessages.getString("ToggleOrientationAction.vertical.label")); //$NON-NLS-1$
            setDescription(CallHierarchyMessages.getString("ToggleOrientationAction.vertical.description")); //$NON-NLS-1$
            setToolTipText(CallHierarchyMessages.getString("ToggleOrientationAction.vertical.tooltip")); //$NON-NLS-1$  
            JavaPluginImages.setLocalImageDescriptors(this, "th_vertical.gif"); //$NON-NLS-1$
        } else if (orientation == CallHierarchyViewPart.VIEW_ORIENTATION_SINGLE) {
            setText(CallHierarchyMessages.getString("ToggleOrientationAction.single.label")); //$NON-NLS-1$
            setDescription(CallHierarchyMessages.getString("ToggleOrientationAction.single.description")); //$NON-NLS-1$
            setToolTipText(CallHierarchyMessages.getString("ToggleOrientationAction.single.tooltip")); //$NON-NLS-1$    
            JavaPluginImages.setLocalImageDescriptors(this, "th_single.gif"); //$NON-NLS-1$
        } else {
            Assert.isTrue(false);
        }
        fView= v;
        fOrientation= orientation;
        WorkbenchHelp.setHelp(this, IJavaHelpContextIds.TOGGLE_ORIENTATION_ACTION);
    }
    
    public int getOrientation() {
        return fOrientation;
    }   
    
    /*
     * @see Action#actionPerformed
     */     
    public void run() {
        fView.setOrientation(fOrientation); // will toggle the checked state
    }
    
}
