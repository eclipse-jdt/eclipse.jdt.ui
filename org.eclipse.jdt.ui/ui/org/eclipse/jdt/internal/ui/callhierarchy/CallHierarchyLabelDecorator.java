/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
 *          (report 36180: Callers/Callees view)
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.callhierarchy;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;

import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.viewsupport.ImageImageDescriptor;

/**
 * Label decorator that decorates an method's image with recursion overlays.
 * The viewer using this decorator is responsible for updating the images on element changes.
 */
public class CallHierarchyLabelDecorator implements ILabelDecorator {

    /**
     * Creates a decorator. The decorator creates an own image registry to cache
     * images.
     */
    public CallHierarchyLabelDecorator() {
        // Do nothing
    }

    @Override
	public String decorateText(String text, Object element) {
        return text;
    }

    @Override
	public Image decorateImage(Image image, Object element) {
		if (image == null)
			return null;

        int adornmentFlags= computeAdornmentFlags(element);
        if (adornmentFlags != 0) {
            ImageDescriptor baseImage= new ImageImageDescriptor(image);
            Rectangle bounds= image.getBounds();
            return JavaPlugin.getImageDescriptorRegistry().get(new CallHierarchyImageDescriptor(baseImage, adornmentFlags, new Point(bounds.width, bounds.height)));
        }
        return image;
    }

    /**
     * Note: This method is for internal use only. Clients should not call this method.
     *
	 * @param element the element for which to compute the flags
	 * @return the flags
     */
    private int computeAdornmentFlags(Object element) {
        int flags= 0;
        if (element instanceof MethodWrapper) {
            MethodWrapper methodWrapper= (MethodWrapper) element;
            if (methodWrapper.isRecursive()) {
                flags= CallHierarchyImageDescriptor.RECURSIVE;
            }
            if (isMaxCallDepthExceeded(methodWrapper)) {
                flags|= CallHierarchyImageDescriptor.MAX_LEVEL;
            }
        }
        return flags;
    }

    private boolean isMaxCallDepthExceeded(MethodWrapper methodWrapper) {
        return methodWrapper.getLevel() > CallHierarchyUI.getDefault().getMaxCallDepth();
    }

    @Override
	public void addListener(ILabelProviderListener listener) {
        // Do nothing
    }

    @Override
	public void dispose() {
        // Nothing to dispose
    }

    @Override
	public boolean isLabelProperty(Object element, String property) {
        return true;
    }

    @Override
	public void removeListener(ILabelProviderListener listener) {
        // Do nothing
    }
}
