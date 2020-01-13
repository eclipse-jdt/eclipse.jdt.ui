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
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.viewsupport;

import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;

import org.eclipse.core.runtime.Assert;


/**
 * Helper class to manage images that should be disposed when a control is disposed
 * contol.addWidgetListener(new ImageDisposer(myImage));
 */
public class ImageDisposer implements DisposeListener {

	private Image[] fImages;

	public ImageDisposer(Image image) {
		this(new Image[] { image });
	}

	public ImageDisposer(Image[] images) {
		Assert.isNotNull(images);
		fImages= images;
	}

	/*
	 * @see WidgetListener#widgetDisposed
	 */
	@Override
	public void widgetDisposed(DisposeEvent e) {
		if (fImages != null) {
			for (Image image : fImages) {
				image.dispose();
			}
		}
	}
}
