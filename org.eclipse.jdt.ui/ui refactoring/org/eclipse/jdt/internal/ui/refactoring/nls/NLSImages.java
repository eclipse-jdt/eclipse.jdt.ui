/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.nls;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class NLSImages {

	private static final String NAME_PREFIX= "org.eclipse.jdt.ui.nls"; //$NON-NLS-1$
	private static final int    NAME_PREFIX_LENGTH= NAME_PREFIX.length();

	// Subdirectory (under the package containing this class) where images are
	private static URL fgIconBaseURL= null;

	static {
		String pathSuffix= ""; //$NON-NLS-1$
		Display display= Display.getCurrent();
		if (display == null)
			// class might get loaded by non-UI thread.
			display= Display.getDefault();
		if (display != null && display.getIconDepth() > 4)
			pathSuffix = "icons/full/"; //$NON-NLS-1$
		else
			pathSuffix = "icons/basic/"; //$NON-NLS-1$
		try {
			fgIconBaseURL= new URL(JavaPlugin.getDefault().getDescriptor().getInstallURL(), pathSuffix);
		} catch (MalformedURLException e) {
			// do nothing
		}
	}

	// The plugin registry
	private final static ImageRegistry IMAGE_REGISTRY= new ImageRegistry();
	
	/**
	 * Available cached Images in the Java plugin image registry.
	 */	

	public static final String IMG_OBJS_NLS_TRANSLATE= NAME_PREFIX + "translate.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_NLS_NEVER_TRANSLATE= NAME_PREFIX + "never_translate.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_NLS_SKIP= NAME_PREFIX + "skip.gif"; //$NON-NLS-1$
	public static final String IMG_OBJS_SEARCH_REF= NAME_PREFIX + "search_ref_obj.gif"; //$NON-NLS-1$

	/**
	 * Set of predefined Image Descriptors.
	 */
	private static final String T_OBJ= "obj16"; //$NON-NLS-1$

	public static final ImageDescriptor DESC_OBJS_NLS_TRANSLATE= createManaged(T_OBJ, IMG_OBJS_NLS_TRANSLATE);
	public static final ImageDescriptor DESC_OBJS_NLS_NEVER_TRANSLATE= createManaged(T_OBJ, IMG_OBJS_NLS_NEVER_TRANSLATE);
	public static final ImageDescriptor DESC_OBJS_NLS_SKIP= createManaged(T_OBJ, IMG_OBJS_NLS_SKIP);
	public static final ImageDescriptor DESC_OBJS_SEARCH_REF= createManaged(T_OBJ, IMG_OBJS_SEARCH_REF);
	
	
	public static Image get(String key) {
		return IMAGE_REGISTRY.get(key);
	}
	
	private static ImageDescriptor createManaged(String prefix, String name) {
		try {
			ImageDescriptor result= ImageDescriptor.createFromURL(makeIconFileURL(prefix, name.substring(NAME_PREFIX_LENGTH)));
			IMAGE_REGISTRY.put(name, result);
			return result;
		} catch (MalformedURLException e) {
			return ImageDescriptor.getMissingImageDescriptor();
		}
	}
	
	private static URL makeIconFileURL(String prefix, String name) throws MalformedURLException {
		if (fgIconBaseURL == null)
			throw new MalformedURLException();
			
		StringBuffer buffer= new StringBuffer(prefix);
		buffer.append('/');
		buffer.append(name);
		return new URL(fgIconBaseURL, buffer.toString());
	}

	static ImageRegistry getImageRegistry() {
		return IMAGE_REGISTRY;
	}
}
