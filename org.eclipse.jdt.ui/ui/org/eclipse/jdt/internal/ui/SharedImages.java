/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui;


import java.util.HashMap;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.jdt.ui.ISharedImages;

/**
 * Default implementation of ISharedImages
 */
public class SharedImages implements ISharedImages {
	
	private static HashMap fgMap;
	
	public SharedImages() {
		if (fgMap == null)
			fillMap();
	}
		
	/* (Non-Javadoc)
	 * Method declared in ISharedImages
	 */
	public Image getImage(String key) {
		return JavaPluginImages.get(key);
	}
	
	/* (Non-Javadoc)
	 * Method declared in ISharedImages
	 */
	public ImageDescriptor getImageDescriptor(String key) {
		return (ImageDescriptor)fgMap.get(key);
	}
	
	private void fillMap() {
		fgMap= new HashMap(20);
		fgMap.put(IMG_OBJS_CUNIT, JavaPluginImages.DESC_OBJS_CUNIT);
		fgMap.put(IMG_OBJS_CFILE, JavaPluginImages.DESC_OBJS_CFILE);
		fgMap.put(IMG_OBJS_JAR, JavaPluginImages.DESC_OBJS_JAR);
		fgMap.put(IMG_OBJS_PACKAGE, JavaPluginImages.DESC_OBJS_PACKAGE);
		fgMap.put(IMG_OBJS_CLASS, JavaPluginImages.DESC_OBJS_CLASS);
		fgMap.put(IMG_OBJS_INTERFACE, JavaPluginImages.DESC_OBJS_INTERFACE);
		fgMap.put(IMG_OBJS_PACKDECL, JavaPluginImages.DESC_OBJS_PACKDECL);
		fgMap.put(IMG_OBJS_IMPCONT, JavaPluginImages.DESC_OBJS_IMPCONT);
		fgMap.put(IMG_OBJS_IMPDECL, JavaPluginImages.DESC_OBJS_IMPDECL);
		fgMap.put(IMG_OBJS_PUBLIC, JavaPluginImages.DESC_MISC_PUBLIC);
		fgMap.put(IMG_OBJS_PROTECTED, JavaPluginImages.DESC_MISC_PROTECTED);
		fgMap.put(IMG_OBJS_PRIVATE, JavaPluginImages.DESC_MISC_PRIVATE);
		fgMap.put(IMG_OBJS_DEFAULT, JavaPluginImages.DESC_MISC_DEFAULT);
		fgMap.put(IMG_OBJS_EXTERNAL_ARCHIVE, JavaPluginImages.DESC_OBJS_EXTJAR);		
		fgMap.put(IMG_OBJS_EXTERNAL_ARCHIVE_WITH_SOURCE, JavaPluginImages.DESC_OBJS_EXTJAR_WSRC);		
	}
}
