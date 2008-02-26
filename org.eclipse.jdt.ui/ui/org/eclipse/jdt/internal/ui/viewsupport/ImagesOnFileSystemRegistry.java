/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.viewsupport;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;

import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.ui.JavaPlugin;


/**
 * Image registry that keeps its images on the local file system.
 * 
 * @since 3.4
 */
public class ImagesOnFileSystemRegistry {
	
	private HashMap fURLMap;
	private final File fTempDir;
	private final JavaElementImageProvider fImageProvider;
	private int fImageCount;

	public ImagesOnFileSystemRegistry() {
		fURLMap= new HashMap();
		fTempDir= getTempDir();
		fImageProvider= new JavaElementImageProvider();
		fImageCount= 0;
	}
	
	private File getTempDir() {
		try {
	        File tempFile = File.createTempFile("jdt-images", "", null);  //$NON-NLS-1$//$NON-NLS-2$
	        if (!tempFile.delete())
	            return null;
	        if (!tempFile.mkdir())
	        	return null;
	        return tempFile;
		} catch (IOException e) {
		}
		return null;
	}
	
	public URL getImageURL(IJavaElement element) {
		ImageDescriptor descriptor= fImageProvider.getJavaImageDescriptor(element, JavaElementImageProvider.OVERLAY_ICONS | JavaElementImageProvider.SMALL_ICONS);
		if (descriptor == null)
			return null;
		return getImageURL(descriptor);
	}
		
	public URL getImageURL(ImageDescriptor descriptor) {
		if (fTempDir == null)
			return null;
		
		URL url= (URL) fURLMap.get(descriptor);
		if (url != null)
			return url;
		
		File file= new File(fTempDir, String.valueOf(fImageCount++) + ".png"); //$NON-NLS-1$
		
		Image image= JavaPlugin.getImageDescriptorRegistry().get(descriptor);
		ImageLoader loader= new ImageLoader();
		loader.data= new ImageData[] { image.getImageData() };
		loader.save(file.getAbsolutePath(), SWT.IMAGE_PNG);
		
		try {
			url= file.toURI().toURL();
			fURLMap.put(descriptor, url);
			return url;
		} catch (MalformedURLException e) {
			JavaPlugin.log(e);
		}
		return null;
	}
	
	public void dispose() {
		if (fTempDir != null) {
			File[] listFiles= fTempDir.listFiles();
			for (int i= 0; i < listFiles.length; i++) {
				listFiles[i].delete();
			}
			fTempDir.delete();
		}
		fURLMap= null;
	}
}
