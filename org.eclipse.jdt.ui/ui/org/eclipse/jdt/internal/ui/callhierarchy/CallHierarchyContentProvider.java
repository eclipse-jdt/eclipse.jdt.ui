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

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;

public class CallHierarchyContentProvider implements ITreeContentProvider {
    private final static Object[] EMPTY_ARRAY = new Object[0];
    private TreeViewer fViewer;

    public CallHierarchyContentProvider() {
        super();
    }

    /**
     * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
     */
    public Object[] getChildren(Object parentElement) {
        if (parentElement instanceof TreeRoot) {
            TreeRoot dummyRoot = (TreeRoot) parentElement;

            return new Object[] { dummyRoot.getRoot() };
        } else if (parentElement instanceof MethodWrapper) {
            MethodWrapper methodWrapper = ((MethodWrapper) parentElement);

            if (!isMaxCallDepthExceeded(methodWrapper)) {
                if (isRecursive(methodWrapper)) {
                    return TreeTermination.RECURSION_NODE.getObjectArray();
                }
                return methodWrapper.getCalls();
            } else {
                return TreeTermination.MAX_CALL_DEPTH_NODE.getObjectArray();
            }
        }

        return EMPTY_ARRAY;
    }

    /**
     * Determines whether the call is recursive
     * @param methodWrapper
     * @return
     */
    private boolean isRecursive(MethodWrapper methodWrapper) {
        return methodWrapper.isRecursive();
    }

    private boolean isMaxCallDepthExceeded(MethodWrapper methodWrapper) {
        return methodWrapper.getLevel() > CallHierarchyUI.getDefault().getMaxCallDepth();
    }

    /**
     * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
     */
    public Object[] getElements(Object inputElement) {
        return getChildren(inputElement);
    }

    /**
     * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
     */
    public Object getParent(Object element) {
        if (element instanceof MethodWrapper) {
            return ((MethodWrapper) element).getParent();
        }

        return null;
    }

    /**
     * @see org.eclipse.jface.viewers.IContentProvider#dispose()
     */
    public void dispose() {}

    /**
     * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
     */
    public boolean hasChildren(Object element) {
        if (element == TreeRoot.EMPTY_ROOT || element == TreeTermination.MAX_CALL_DEPTH_NODE || element == TreeTermination.RECURSION_NODE) {
            return false;
        }
        // Only methods can have subelements, so there's no need to fool the user into believing that there is more
        if (element instanceof MethodWrapper) {
            return ((MethodWrapper)element).getMember().getElementType() == IJavaElement.METHOD;
        }

        return true;
    }

    /**
     * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
     */
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        this.fViewer = (TreeViewer) viewer;
    }
}
