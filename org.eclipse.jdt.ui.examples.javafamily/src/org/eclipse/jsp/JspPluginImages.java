/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.jsp;


import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;

import org.eclipse.debug.core.DebugPlugin;

/**
 * The images provided by the debug plugin.
 */
public class JspPluginImages {

	/**
	 * The image registry containing <code>Image</code>s.
	 */
	private static ImageRegistry imageRegistry;

	/**
	 * A table of all the <code>ImageDescriptor</code>s.
	 */
	private static HashMap imageDescriptors;

	/* Declare Common paths */
	private static URL ICON_BASE_URL= null;

	static {
		String pathSuffix = "icons/full/"; //$NON-NLS-1$

		try {
			ICON_BASE_URL= new URL(JspUIPlugin.getDefault().getBundle().getEntry("/"), pathSuffix); //$NON-NLS-1$
		} catch (MalformedURLException e) {
			// do nothing
		}
	}

	// Use IPath and toOSString to build the names to ensure they have the slashes correct
	//private final static String CTOOL= "ctool16/"; //basic colors - size 16x16 //$NON-NLS-1$
	//private final static String LOCALTOOL= "clcl16/"; //basic colors - size 16x16 //$NON-NLS-1$
	//private final static String DLCL= "dlcl16/"; //disabled - size 16x16 //$NON-NLS-1$
	//private final static String ELCL= "elcl16/"; //enabled - size 16x16 //$NON-NLS-1$
	private final static String OBJECT= "obj16/"; //basic colors - size 16x16 //$NON-NLS-1$
	//private final static String WIZBAN= "wizban/"; //basic colors - size 16x16 //$NON-NLS-1$
	//private final static String OVR= "ovr16/"; //basic colors - size 7x8 //$NON-NLS-1$
	//private final static String VIEW= "cview16/"; // views //$NON-NLS-1$

	public final static String IMG_OBJ_TOMCAT = "IMG_TOMCAT"; //$NON-NLS-1$
	public final static String IMG_OBJ_JSP = "IMG_OBJ_JSP"; //$NON-NLS-1$

	/**
	 * Declare all images
	 */
	private static void declareImages() {

		declareRegistryImage(IMG_OBJ_TOMCAT, OBJECT + "tomcat_obj.gif"); //$NON-NLS-1$
		declareRegistryImage(IMG_OBJ_JSP, OBJECT + "jsp_obj.gif"); //$NON-NLS-1$
	}

	/**
	 * Declare an Image in the registry table.
	 * @param key 	The key to use when registering the image
	 * @param path	The path where the image can be found. This path is relative to where
	 *				this plugin class is found (i.e. typically the packages directory)
	 */
	private final static void declareRegistryImage(String key, String path) {
		ImageDescriptor desc= ImageDescriptor.getMissingImageDescriptor();
		try {
			desc= ImageDescriptor.createFromURL(makeIconFileURL(path));
		} catch (MalformedURLException me) {
			DebugPlugin.log(me);
		}
		imageRegistry.put(key, desc);
		imageDescriptors.put(key, desc);
	}

	/**
	 * Returns the ImageRegistry.
	 */
	public static ImageRegistry getImageRegistry() {
		if (imageRegistry == null) {
			initializeImageRegistry();
		}
		return imageRegistry;
	}

	/**
	 *	Initialize the image registry by declaring all of the required
	 *	graphics. This involves creating JFace image descriptors describing
	 *	how to create/find the image should it be needed.
	 *	The image is not actually allocated until requested.
	 *
	 * 	Prefix conventions
	 *		Wizard Banners			WIZBAN_
	 *		Preference Banners		PREF_BAN_
	 *		Property Page Banners	PROPBAN_
	 *		Color toolbar			CTOOL_
	 *		Enable toolbar			ETOOL_
	 *		Disable toolbar			DTOOL_
	 *		Local enabled toolbar	ELCL_
	 *		Local Disable toolbar	DLCL_
	 *		Object large			OBJL_
	 *		Object small			OBJS_
	 *		View 					VIEW_
	 *		Product images			PROD_
	 *		Misc images				MISC_
	 *
	 *	Where are the images?
	 *		The images (typically gifs) are found in the same location as this plugin class.
	 *		This may mean the same package directory as the package holding this class.
	 *		The images are declared using this.getClass() to ensure they are looked up via
	 *		this plugin class.
	 *	see JFace's ImageRegistry
	 */
	public static ImageRegistry initializeImageRegistry() {
		imageRegistry= new ImageRegistry(JspUIPlugin.getStandardDisplay());
		imageDescriptors = new HashMap<>(30);
		declareImages();
		return imageRegistry;
	}

	/**
	 * Returns the <code>Image</code> identified by the given key,
	 * or <code>null</code> if it does not exist.
	 */
	public static Image getImage(String key) {
		return getImageRegistry().get(key);
	}

	/**
	 * Returns the <code>ImageDescriptor</code> identified by the given key,
	 * or <code>null</code> if it does not exist.
	 */
	public static ImageDescriptor getImageDescriptor(String key) {
		if (imageDescriptors == null) {
			initializeImageRegistry();
		}
		return (ImageDescriptor)imageDescriptors.get(key);
	}

	private static URL makeIconFileURL(String iconPath) throws MalformedURLException {
		if (ICON_BASE_URL == null) {
			throw new MalformedURLException();
		}

		return new URL(ICON_BASE_URL, iconPath);
	}
}


