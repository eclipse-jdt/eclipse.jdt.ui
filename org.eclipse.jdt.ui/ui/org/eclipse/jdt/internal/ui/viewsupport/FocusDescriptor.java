/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.viewsupport;

import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * An image descriptor that draws a focus adornment on top of a base image.
 *
 * @since 3.7
 */
public class FocusDescriptor extends CompositeImageDescriptor {
	private ImageDescriptor fBase;
	public FocusDescriptor(ImageDescriptor base) {
		fBase= base;
	}
	@Override
	protected void drawCompositeImage(int width, int height) {
		drawImage(createCachedImageDataProvider(fBase), 0, 0);
		drawImage(createCachedImageDataProvider(JavaPluginImages.DESC_OVR_FOCUS), 0, 0);
	}

	@Override
	protected Point getSize() {
		return JavaElementImageProvider.BIG_SIZE;
	}
	@Override
	public int hashCode() {
		return fBase.hashCode();
	}
	@Override
	public boolean equals(Object object) {
		return object != null && FocusDescriptor.class.equals(object.getClass()) && ((FocusDescriptor)object).fBase.equals(fBase);
	}
}
