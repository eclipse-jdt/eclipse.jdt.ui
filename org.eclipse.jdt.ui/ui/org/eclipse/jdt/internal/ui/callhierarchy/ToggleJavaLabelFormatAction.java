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
 * Toggles the format of the Java labels of the call hierarchy
 */
class ToggleJavaLabelFormatAction extends Action {

    private CallHierarchyViewPart fView;    
    private int fFormat;
    
    public ToggleJavaLabelFormatAction(CallHierarchyViewPart v, int format) {
        super("", AS_RADIO_BUTTON); //$NON-NLS-1$
        if (format == CallHierarchyViewPart.JAVA_LABEL_FORMAT_DEFAULT) {
            setText(CallHierarchyMessages.getString("ToggleJavaLabelFormatAction.default.label")); //$NON-NLS-1$
            setDescription(CallHierarchyMessages.getString("ToggleJavaLabelFormatAction.default.description")); //$NON-NLS-1$
            setToolTipText(CallHierarchyMessages.getString("ToggleJavaLabelFormatAction.default.tooltip")); //$NON-NLS-1$
            JavaPluginImages.setLocalImageDescriptors(this, "th_horizontal.gif"); //$NON-NLS-1$
        } else if (format == CallHierarchyViewPart.JAVA_LABEL_FORMAT_SHORT) {
            setText(CallHierarchyMessages.getString("ToggleJavaLabelFormatAction.short.label")); //$NON-NLS-1$
            setDescription(CallHierarchyMessages.getString("ToggleJavaLabelFormatAction.short.description")); //$NON-NLS-1$
            setToolTipText(CallHierarchyMessages.getString("ToggleJavaLabelFormatAction.short.tooltip")); //$NON-NLS-1$  
            JavaPluginImages.setLocalImageDescriptors(this, "th_vertical.gif"); //$NON-NLS-1$
        } else if (format == CallHierarchyViewPart.JAVA_LABEL_FORMAT_LONG) {
            setText(CallHierarchyMessages.getString("ToggleJavaLabelFormatAction.long.label")); //$NON-NLS-1$
            setDescription(CallHierarchyMessages.getString("ToggleJavaLabelFormatAction.long.description")); //$NON-NLS-1$
            setToolTipText(CallHierarchyMessages.getString("ToggleJavaLabelFormatAction.long.tooltip")); //$NON-NLS-1$    
            JavaPluginImages.setLocalImageDescriptors(this, "th_single.gif"); //$NON-NLS-1$
        } else {
            Assert.isTrue(false);
        }
        fView= v;
        fFormat= format;
        WorkbenchHelp.setHelp(this, IJavaHelpContextIds.CALL_HIERARCHY_TOGGLE_JAVA_LABEL_FORMAT_ACTION);
    }
    
    public int getFormat() {
        return fFormat;
    }   
    
    /*
     * @see Action#actionPerformed
     */     
    public void run() {
        fView.setJavaLabelFormat(fFormat); // will toggle the checked state
    }
    
}
