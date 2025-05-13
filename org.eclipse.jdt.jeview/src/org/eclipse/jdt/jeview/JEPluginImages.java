/*******************************************************************************
 * Copyright (c) 2005, 2025 IBM Corporation and others.
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

package org.eclipse.jdt.jeview;

import org.eclipse.jface.resource.ImageDescriptor;


public class JEPluginImages {

	private static final String fgIconBase= "/icons/c/";

	public static final String CHILDREN= "children.svg";
	public static final String INFO= "info.svg";
	public static final String PROPERTIES= "properties.svg";
	public static final String REFRESH= "refresh.svg";
	public static final String SET_FOCUS= "setfocus.svg";
	public static final String CODE_SELECT= "codeSelect.svg";

	public static final ImageDescriptor IMG_CHILDREN= create(CHILDREN);
	public static final ImageDescriptor IMG_INFO= create(INFO);

	public static final ImageDescriptor IMG_PROPERTIES= create(PROPERTIES);
	public static final ImageDescriptor IMG_REFRESH= create(REFRESH);
	public static final ImageDescriptor IMG_SET_FOCUS= create(SET_FOCUS);
	public static final ImageDescriptor IMG_SET_FOCUS_CODE_SELECT= create(CODE_SELECT);

	private static ImageDescriptor create(String name) {
		return ImageDescriptor.createFromURL(JEViewPlugin.getDefault().getBundle().getEntry(fgIconBase+ name));
	}

	private JEPluginImages() {
	}
}
