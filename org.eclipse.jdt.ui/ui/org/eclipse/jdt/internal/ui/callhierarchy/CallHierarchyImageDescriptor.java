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
 *          (report 36180: Callers/Callees view)
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.callhierarchy;

import org.eclipse.swt.graphics.Point;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.jdt.internal.ui.JavaPluginImages;


public class CallHierarchyImageDescriptor extends CompositeImageDescriptor {

    /** Flag to render the recursive adornment */
    public final static int RECURSIVE=       0x001;

    /** Flag to render the callee adornment */
    public final static int MAX_LEVEL=       0x002;

    private ImageDescriptor fBaseImage;
    private int fFlags;
    private Point fSize;

    /**
     * Creates a new CallHierarchyImageDescriptor.
     *
     * @param baseImage an image descriptor used as the base image
     * @param flags flags indicating which adornments are to be rendered. See <code>setAdornments</code>
     *  for valid values.
     * @param size the size of the resulting image
     */
    public CallHierarchyImageDescriptor(ImageDescriptor baseImage, int flags, Point size) {
        fBaseImage= baseImage;
        Assert.isNotNull(fBaseImage);
        fFlags= flags;
        Assert.isTrue(fFlags >= 0);
        fSize= size;
        Assert.isNotNull(fSize);
    }

    @Override
	protected Point getSize() {
        return fSize;
    }

    @Override
	public boolean equals(Object object) {
        if (object == null || !CallHierarchyImageDescriptor.class.equals(object.getClass()))
            return false;

        CallHierarchyImageDescriptor other= (CallHierarchyImageDescriptor)object;
        return (fBaseImage.equals(other.fBaseImage) && fFlags == other.fFlags && fSize.equals(other.fSize));
    }

    @Override
	public int hashCode() {
        return fBaseImage.hashCode() | fFlags | fSize.hashCode();
    }

    @Override
	protected void drawCompositeImage(int width, int height) {
        CachedImageDataProvider bg= createCachedImageDataProvider(fBaseImage);

        drawImage(bg, 0, 0);
        drawBottomLeft();
    }

	private void drawBottomLeft() {
        Point size= getSize();
        int x= 0;
        CachedImageDataProvider dataProvider= null;
        if ((fFlags & RECURSIVE) != 0) {
            dataProvider= createCachedImageDataProvider(JavaPluginImages.DESC_OVR_RECURSIVE);
            drawImage(dataProvider, x, size.y - dataProvider.getHeight());
            x+= dataProvider.getWidth();
        }
        if ((fFlags & MAX_LEVEL) != 0) {
            dataProvider= createCachedImageDataProvider(JavaPluginImages.DESC_OVR_MAX_LEVEL);
            drawImage(dataProvider, x, size.y - dataProvider.getHeight());
            x+= dataProvider.getWidth();
        }
    }
}
