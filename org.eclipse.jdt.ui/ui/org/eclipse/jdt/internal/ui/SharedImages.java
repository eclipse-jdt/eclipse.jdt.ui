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
package org.eclipse.jdt.internal.ui;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.jdt.ui.ISharedImages;

/**
 * Default implementation of ISharedImages
 */
public class SharedImages implements ISharedImages {

	public SharedImages() {
	}

	@Override
	public Image getImage(String key) {
		return JavaPluginImages.get(key);
	}

	@Override
	public ImageDescriptor getImageDescriptor(String key) {
		return JavaPluginImages.getDescriptor(key);
	}

}
