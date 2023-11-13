/*******************************************************************************
* Copyright (c) 2024 Jozef Tomek and others.
*
* This program and the accompanying materials
* are made available under the terms of the Eclipse Public License 2.0
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Jozef Tomek - initial API and implementation
*******************************************************************************/
package org.eclipse.jdt.internal.ui.viewsupport.javadoc;

import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * Image descriptor adding mouse pointer overlay to the bottom right corner of the image
 */
class JavadocEnrichmentImageDescriptor extends CompositeImageDescriptor {
	private final ImageDescriptor baseImage;
	private final Point size;

	public JavadocEnrichmentImageDescriptor(ImageDescriptor baseImage) {
		this.baseImage= baseImage;
		CachedImageDataProvider provider= createCachedImageDataProvider(baseImage);
		size= new Point(provider.getWidth(), provider.getHeight());
	}

	@Override
	protected void drawCompositeImage(int width, int height) {
		drawImage(createCachedImageDataProvider(baseImage), 0, 0);
		drawCursorOverlay();
	}

	@Override
	protected Point getSize() {
		return size;
	}

	private void drawCursorOverlay() {
		CachedImageDataProvider provider= createCachedImageDataProvider(JavaPluginImages.DESC_OVR_MOUSE_CURSOR);
		int x= size.x - provider.getWidth();
		int y= size.y - provider.getHeight();
		if (x >= 0 && y >= 0) {
			drawImage(provider, x, y);
		}
	}

}