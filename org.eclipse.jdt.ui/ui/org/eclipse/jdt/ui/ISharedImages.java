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
package org.eclipse.jdt.ui;


import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * Standard images provided by the Java UI plug-in. This class offers access to the 
 * standard images in two forms:
 * <ul>
 *   <li>Use <code>ISharedImages.getImage(IMG_OBJS_<it>FOO</it>)</code> 
 *    to access the shared standard <code>Image</code> object (caller must not
 *    dispose of image).</li>
 *   <li>Use <code>ISharedImages.getImageDescriptor(IMG_OBJS_<it>FOO</it>)</code> 
 *    to access the standard <code>ImageDescriptor</code> object (caller is 
 *    responsible for disposing of any <code>Image</code> objects it creates using
 *    this descriptor).</li>
 * </ul>
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p> 
 */
public interface ISharedImages {
			
	/** Key to access the shared image or image descriptor for a Java compilation unit. */
	public static final String IMG_OBJS_CUNIT= JavaPluginImages.IMG_OBJS_CUNIT;
	
	/** Key to access the shared image or image descriptor for a Java class file. */
	public static final String IMG_OBJS_CFILE= JavaPluginImages.IMG_OBJS_CFILE; 
	
	/** Key to access the shared image or image descriptor for a JAR. */
	public static final String IMG_OBJS_JAR= JavaPluginImages.IMG_OBJS_JAR;
	
	/**
	 * Key to access the shared image or image descriptor for a JAR with source.
	 * @since 3.0
	 */
	public static final String IMG_OBJS_JAR_WITH_SOURCE= JavaPluginImages.IMG_OBJS_JAR_WSRC;
	
	/**
	 * Key to access the shared image or image descriptor for external archives. 
	 * @since 2.1
	 */
	public static final String IMG_OBJS_EXTERNAL_ARCHIVE= JavaPluginImages.IMG_OBJS_EXTJAR;
	
	/** 
	 * Key to access the shared image or image descriptor for external archives with source.
	 * @since 2.1
	 */
	public static final String IMG_OBJS_EXTERNAL_ARCHIVE_WITH_SOURCE= JavaPluginImages.IMG_OBJS_EXTJAR_WSRC;

	/**
	 * Key to access the shared image or image descriptor for a library (class path container).
	 * @since 3.0
	 */
	public static final String IMG_OBJS_LIBRARY= JavaPluginImages.IMG_OBJS_LIBRARY;
	
	/**
	 * Key to access the shared image or image descriptor for a package fragment root.
	 * @since 3.0
	 */
	public static final String IMG_OBJS_PACKFRAG_ROOT= JavaPluginImages.IMG_OBJS_PACKFRAG_ROOT;
	
	/**
	 * Key to access the shared image or image descriptor for a package.
	 */
	public static final String IMG_OBJS_PACKAGE= JavaPluginImages.IMG_OBJS_PACKAGE;
	
	/**
	 * Key to access the shared image or image descriptor for an empty package.
	 * @since 3.0
	 */
	public static final String IMG_OBJS_EMPTY_PACKAGE= JavaPluginImages.IMG_OBJS_EMPTY_PACKAGE;

	/**
	 * Key to access the shared image or image descriptor for a logical package.
	 * @since 3.0
	 */
	public static final String IMG_OBJS_LOGICAL_PACKAGE= JavaPluginImages.IMG_OBJS_LOGICAL_PACKAGE;
	
	/**
	 * Key to access the shared image or image descriptor for an empty logical package.
	 * @since 3.0
	 */
	public static final String IMG_OBJS_EMPTY_LOGICAL_PACKAGE= JavaPluginImages.IMG_OBJS_EMPTY_LOGICAL_PACKAGE;
	
	/**
	 * Key to access the shared image or image descriptor for a class.
	 */	
	public static final String IMG_OBJS_CLASS= JavaPluginImages.IMG_OBJS_CLASS;
	
	/**
	 * Key to access the shared image or image descriptor for a class with default visibility.
	 * @since 3.0
	 */
	public static final String IMG_OBJS_CLASS_DEFAULT= JavaPluginImages.IMG_OBJS_CLASS_DEFAULT;
	
	/**
	 * Key to access the shared image or image descriptor for a public inner class.
	 * @since 3.0
	 */
	public static final String IMG_OBJS_INNER_CLASS_PUBLIC= JavaPluginImages.IMG_OBJS_INNER_CLASS_PUBLIC;
	
	/**
	 * Key to access the shared image or image descriptor for a inner class with default visibility.
	 * @since 3.0
	 */
	public static final String IMG_OBJS_INNER_CLASS_DEFAULT= JavaPluginImages.IMG_OBJS_INNER_CLASS_DEFAULT;
	
	/**
	 * Key to access the shared image or image descriptor for a protected inner class.
	 * @since 3.0
	 */
	public static final String IMG_OBJS_INNER_CLASS_PROTECTED= JavaPluginImages.IMG_OBJS_INNER_CLASS_PROTECTED;
	
	/**
	 * Key to access the shared image or image descriptor for a private inner class.
	 * @since 3.0
	 */
	public static final String IMG_OBJS_INNER_CLASS_PRIVATE= JavaPluginImages.IMG_OBJS_INNER_CLASS_PRIVATE;
	
	/**
	 * Key to access the shared image or image descriptor for an interface.
	 */
	public static final String IMG_OBJS_INTERFACE= JavaPluginImages.IMG_OBJS_INTERFACE;
	
	/**
	 * Key to access the shared image or image descriptor for an interface with default visibility.
	 * @since 3.0
	 */
	public static final String IMG_OBJS_INTERFACE_DEFAULT= JavaPluginImages.IMG_OBJS_INTERFACE_DEFAULT;
	
	/**
	 * Key to access the shared image or image descriptor for a public inner interface.
	 * @since 3.0
	 */
	public static final String IMG_OBJS_INNER_INTERFACE_PUBLIC= JavaPluginImages.IMG_OBJS_INNER_INTERFACE_PUBLIC;
	
	/**
	 * Key to access the shared image or image descriptor for an inner interface with default visibility.
	 * @since 3.0
	 */
	public static final String IMG_OBJS_INNER_INTERFACE_DEFAULT= JavaPluginImages.IMG_OBJS_INNER_INTERFACE_DEFAULT;
	
	/**
	 * Key to access the shared image or image descriptor for a protected inner interface.
	 * @since 3.0
	 */
	public static final String IMG_OBJS_INNER_INTERFACE_PROTECTED= JavaPluginImages.IMG_OBJS_INNER_INTERFACE_PROTECTED;
	
	/**
	 * Key to access the shared image or image descriptor for a private inner interface.
	 * @since 3.0
	 */
	public static final String IMG_OBJS_INNER_INTERFACE_PRIVATE= JavaPluginImages.IMG_OBJS_INNER_INTERFACE_PRIVATE;

	/** Key to access the shared image or image descriptor for a package declaration. */
	public static final String IMG_OBJS_PACKDECL= JavaPluginImages.IMG_OBJS_PACKDECL;
	
	/** Key to access the shared image or image descriptor for an import container. */
	public static final String IMG_OBJS_IMPCONT= JavaPluginImages.IMG_OBJS_IMPCONT;
	
	/** Key to access the shared image or image descriptor for an import statement. */
	public static final String IMG_OBJS_IMPDECL= JavaPluginImages.IMG_OBJS_IMPDECL;
	
	/** Key to access the shared image or image descriptor for a public member. */
	public static final String IMG_OBJS_PUBLIC= JavaPluginImages.IMG_MISC_PUBLIC;
	
	/** Key to access the shared image or image descriptor for a protected member. */
	public static final String IMG_OBJS_PROTECTED= JavaPluginImages.IMG_MISC_PROTECTED;
	
	/** Key to access the shared image or image descriptor for a private member. */
	public static final String IMG_OBJS_PRIVATE= JavaPluginImages.IMG_MISC_PRIVATE;
	
	/** Key to access the shared image or image descriptor for class members with default visibility. */
	public static final String IMG_OBJS_DEFAULT= JavaPluginImages.IMG_MISC_DEFAULT;
	
	/**
	 * Key to access the shared image or image descriptor for a public field.
	 * @since 3.0
	 */
	public static final String IMG_FIELD_PUBLIC= JavaPluginImages.IMG_FIELD_PUBLIC;
	
	/**
	 * Key to access the shared image or image descriptor for a protected field.
	 * @since 3.0
	 */
	public static final String IMG_FIELD_PROTECTED= JavaPluginImages.IMG_FIELD_PROTECTED;
	
	/**
	 * Key to access the shared image or image descriptor for a private field.
	 * @since 3.0
	 */
	public static final String IMG_FIELD_PRIVATE= JavaPluginImages.IMG_FIELD_PRIVATE;
	
	/**
	 * Key to access the shared image or image descriptor for a field with default visibility.
	 * @since 3.0
	 */
	public static final String IMG_FIELD_DEFAULT= JavaPluginImages.IMG_FIELD_DEFAULT;
	
	
	/**
	 * Key to access the shared image or image descriptor for a local variable.
	 * @since 3.0
	 */
	public static final String IMG_OBJS_LOCAL_VARIABLE= JavaPluginImages.IMG_OBJS_LOCAL_VARIABLE;
	
	/**
	 * Returns the shared image managed under the given key.
	 * <p>
	 * Note that clients <b>must not</b> dispose the image returned by this method.
	 * </p>
	 *
	 * @param key the image key; one of the <code>IMG_OBJS_* </code> constants
	 * @return the shared image managed under the given key, or <code>null</code>
	 *   if none
	 */
	public Image getImage(String key);
	
	/**
	 * Returns the image descriptor managed under the given key.
	 *
	 * @param key the image key; one of the <code>IMG_OBJS_* </code> constants
	 * @return the image descriptor managed under the given key, or <code>null</code>
	 *  if none
	 */
	public ImageDescriptor getImageDescriptor(String key);
}
