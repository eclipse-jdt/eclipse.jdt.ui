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
import org.eclipse.ui.model.IWorkbenchAdapter;

import org.eclipse.jface.viewers.ILabelDecorator;

import org.eclipse.jdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;

import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;

class CallHierarchyLabelProvider extends AppearanceAwareLabelProvider {
    private static final int TEXTFLAGS= DEFAULT_TEXTFLAGS | JavaElementLabels.ALL_POST_QUALIFIED | JavaElementLabels.P_COMPRESSED;
    private static final int IMAGEFLAGS= DEFAULT_IMAGEFLAGS | JavaElementImageProvider.SMALL_ICONS;

    private ILabelDecorator fDecorator;
    
    CallHierarchyLabelProvider() {
        super(TEXTFLAGS, IMAGEFLAGS);
        fDecorator= new CallHierarchyLabelDecorator();
    }
    /**
     * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
     */
    public Image getImage(Object element) {
        Image result= null;
        if (element instanceof MethodWrapper) {
            MethodWrapper methodWrapper = (MethodWrapper) element;

            if (methodWrapper.getMember() != null) {
                result= fDecorator.decorateImage(super.getImage(methodWrapper.getMember()), methodWrapper);
            }
        } else if (isPendingUpdate(element)) {
            return null;
        } else {
            result= super.getImage(element);
        }
        
        return result;
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
        } else if (element == TreeTermination.SEARCH_CANCELED) {
            return CallHierarchyMessages.getString("CallHierarchyLabelProvider.searchCanceled"); //$NON-NLS-1$
        } else if (isPendingUpdate(element)) {
            return CallHierarchyMessages.getString("CallHierarchyLabelProvider.updatePending"); //$NON-NLS-1$
        }

        return CallHierarchyMessages.getString("CallHierarchyLabelProvider.noMethodSelected"); //$NON-NLS-1$
    }

    private boolean isPendingUpdate(Object element) {
        return element instanceof IWorkbenchAdapter;
    }
    private String getElementLabel(MethodWrapper methodWrapper) {
        String label = super.getText(methodWrapper.getMember());

        Collection callLocations = methodWrapper.getMethodCall().getCallLocations();

        if ((callLocations != null) && (callLocations.size() > 1)) {
            return CallHierarchyMessages.getFormattedString("CallHierarchyLabelProvider.matches", new String[]{label, String.valueOf(callLocations.size())}); //$NON-NLS-1$
        }

        return label;
    }
}
