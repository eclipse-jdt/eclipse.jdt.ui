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

import java.util.Collection;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;

class CallHierarchyLabelProvider extends LabelProvider {
    private JavaElementLabelProvider fJavaElementLabelProvider = new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_BASICS);

    /**
     * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
     */
    public Image getImage(Object element) {
        if (element instanceof MethodWrapper) {
            MethodWrapper methodWrapper = (MethodWrapper) element;

            if (methodWrapper.getMember() != null) {
                return fJavaElementLabelProvider.getImage(methodWrapper.getMember());
            }
        }

        return super.getImage(element);
    }

    /*
     * @see ILabelProvider#getText(Object)
     */
    public String getText(Object element) {
        if (element instanceof MethodWrapper) {
            MethodWrapper methodWrapper = (MethodWrapper) element;

            if (methodWrapper.getMember() != null) {
                return getElementLabel(methodWrapper);
            } else {
                return CallHierarchyMessages.getString("CallHierarchyLabelProvider.root"); //$NON-NLS-1$
            }
        } else if (element == TreeTermination.MAX_CALL_DEPTH_NODE) {
            return CallHierarchyMessages.getString("CallHierarchyLabelProvider.maxLevelReached"); //$NON-NLS-1$
        } else if (element == TreeTermination.RECURSION_NODE) {
            return CallHierarchyMessages.getString("CallHierarchyLabelProvider.recursion"); //$NON-NLS-1$
        }

        return CallHierarchyMessages.getString("CallHierarchyLabelProvider.noMethodSelected"); //$NON-NLS-1$
    }

    /**
     * @see org.eclipse.jface.viewers.LabelProvider#dispose()
     */
    public void dispose() {
        super.dispose();

        disposeJavaLabelProvider();
    }

    /**
     * Updates the Java label provider with the new settings.
     */
    void setJavaLabelFormat(int format) {
        fJavaElementLabelProvider.turnOff(Integer.MAX_VALUE);
        fJavaElementLabelProvider.turnOn(format);
    }

    private String getElementLabel(MethodWrapper methodWrapper) {
        String label = fJavaElementLabelProvider.getText(methodWrapper.getMember());

        Collection callLocations = methodWrapper.getMethodCall().getCallLocations();

        if ((callLocations != null) && (callLocations.size() > 1)) {
            return CallHierarchyMessages.getFormattedString("CallHierarchyLabelProvider.matches", new String[]{label, String.valueOf(callLocations.size())}); //$NON-NLS-1$
        }

        return label;
    }

    private void disposeJavaLabelProvider() {
        if (fJavaElementLabelProvider != null) {
            fJavaElementLabelProvider.dispose();
            fJavaElementLabelProvider= null;
        }
    }
}
