/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.viewsupport;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;

import org.eclipse.jface.resource.ImageDescriptor;

/**
  */
public class ImageImageDescriptor extends ImageDescriptor {

	private Image fImage;

	/**
	 * Constructor for ImagImageDescriptor.
	 * @param image the image
	 */
	public ImageImageDescriptor(Image image) {
		super();
		fImage= image;
	}

	@Override
	public ImageData getImageData(int zoom) {
		// workaround for the missing API Image#getImageData(int zoom) (bug 496409)
		if (zoom == 100) {
			return fImage.getImageData();
		}
		ImageData zoomedImageData = fImage.getImageDataAtCurrentZoom();
		Rectangle bounds = fImage.getBounds();
		//TODO: Probably has off-by-one problems at fractional zoom levels:
		if (bounds.width == scaleDown(zoomedImageData.width, zoom)
				&& bounds.height == scaleDown(zoomedImageData.height, zoom)) {
			return zoomedImageData;
		}
		return null;
	}

	private static int scaleDown(int value, int zoom) {
		// @see SWT's internal DPIUtil#autoScaleDown(int)
		float scaleFactor = zoom / 100f;
		return Math.round(value / scaleFactor);
	}

	@Override
	public boolean equals(Object obj) {
		return (obj != null) && getClass().equals(obj.getClass()) && fImage.equals(((ImageImageDescriptor)obj).fImage);
	}

	@Override
	public int hashCode() {
		return fImage.hashCode();
	}

}
