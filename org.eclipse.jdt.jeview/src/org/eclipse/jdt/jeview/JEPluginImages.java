/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.jeview;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;


public class JEPluginImages {

	private static URL fgIconBaseURL= null;
	static {
		fgIconBaseURL= JEViewPlugin.getDefault().getBundle().getEntry("/icons/c/"); //$NON-NLS-1$
	}
	
	public static final String CHILDREN= "children.png";
	public static final String INFO= "info.png";
	
	public static final ImageDescriptor IMG_CHILDREN= create(CHILDREN);
	public static final ImageDescriptor IMG_INFO= create(INFO);
	
	private static ImageDescriptor create(String name) {
		try {
			return ImageDescriptor.createFromURL(new URL(fgIconBaseURL, name));
		} catch (MalformedURLException e) {
			return ImageDescriptor.getMissingImageDescriptor();
		}
	}	
}
