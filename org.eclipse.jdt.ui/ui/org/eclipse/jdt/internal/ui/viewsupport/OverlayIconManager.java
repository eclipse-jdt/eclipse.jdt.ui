/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.viewsupport;


import java.util.HashMap;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;

import org.eclipse.ui.ISharedImages;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * The OverlayIconManager constructs and manages overlay icons. It uses an 
 * IOverlayDescriptorFactory (and the created IOverlayDescriptors) to construct
 * overlay icons. The constructed icon are registered with the plugin's
 * image registry. Thus, constructed icons are destroyed automatically on shutdown.
 * Icons that already exist are reused and not created.
 */
public class OverlayIconManager {
	
	private static final HashMap fgBaseImageDescriptors= new HashMap(10);	
	
	static {
		manage(JavaPluginImages.IMG_MISC_PUBLIC, JavaPluginImages.DESC_MISC_PUBLIC);
		manage(JavaPluginImages.IMG_MISC_PROTECTED, JavaPluginImages.DESC_MISC_PROTECTED);
		manage(JavaPluginImages.IMG_MISC_PRIVATE, JavaPluginImages.DESC_MISC_PRIVATE);
		manage(JavaPluginImages.IMG_MISC_DEFAULT, JavaPluginImages.DESC_MISC_DEFAULT);
		manage(JavaPluginImages.IMG_OBJS_PACKDECL, JavaPluginImages.DESC_OBJS_PACKDECL);
		manage(JavaPluginImages.IMG_OBJS_IMPDECL, JavaPluginImages.DESC_OBJS_IMPDECL);
		manage(JavaPluginImages.IMG_OBJS_IMPCONT, JavaPluginImages.DESC_OBJS_IMPCONT);
		manage(JavaPluginImages.IMG_OBJS_CLASS, JavaPluginImages.DESC_OBJS_CLASS);
		manage(JavaPluginImages.IMG_OBJS_PCLASS, JavaPluginImages.DESC_OBJS_PCLASS);
		manage(JavaPluginImages.IMG_OBJS_INTERFACE, JavaPluginImages.DESC_OBJS_INTERFACE);
		manage(JavaPluginImages.IMG_OBJS_PINTERFACE, JavaPluginImages.DESC_OBJS_PINTERFACE);
		manage(JavaPluginImages.IMG_OBJS_EXTJAR, JavaPluginImages.DESC_OBJS_EXTJAR);
		manage(JavaPluginImages.IMG_OBJS_JAR, JavaPluginImages.DESC_OBJS_JAR);
		manage(JavaPluginImages.IMG_OBJS_EXTJAR_WSRC, JavaPluginImages.DESC_OBJS_EXTJAR_WSRC);
		manage(JavaPluginImages.IMG_OBJS_JAR_WSRC, JavaPluginImages.DESC_OBJS_JAR_WSRC);		
		manage(JavaPluginImages.IMG_OBJS_PACKAGE, JavaPluginImages.DESC_OBJS_PACKAGE);
		manage(JavaPluginImages.IMG_OBJS_PACKFRAG_ROOT, JavaPluginImages.DESC_OBJS_PACKFRAG_ROOT);
		manage(JavaPluginImages.IMG_OBJS_CUNIT, JavaPluginImages.DESC_OBJS_CUNIT);
		manage(JavaPluginImages.IMG_OBJS_CFILE, JavaPluginImages.DESC_OBJS_CFILE);
		manage(JavaPluginImages.IMG_OBJS_CFILECLASS, JavaPluginImages.DESC_OBJS_CFILECLASS);
		manage(JavaPluginImages.IMG_OBJS_CFILEINT, JavaPluginImages.DESC_OBJS_CFILEINT);
		manage(JavaPluginImages.IMG_OBJS_GHOST, JavaPluginImages.DESC_OBJS_GHOST);
		manage(JavaPluginImages.IMG_OBJS_ENV_VAR, JavaPluginImages.DESC_OBJS_ENV_VAR);
		ISharedImages images= JavaPlugin.getDefault().getWorkbench().getSharedImages(); 
		
		// manage some icons from the desktop (because of overlays).
		manage(ISharedImages.IMG_OBJ_PROJECT_CLOSED, images.getImageDescriptor(ISharedImages.IMG_OBJ_PROJECT_CLOSED));
		manage(ISharedImages.IMG_OBJ_PROJECT, images.getImageDescriptor(ISharedImages.IMG_OBJ_PROJECT));
		manage(ISharedImages.IMG_OBJ_FOLDER, images.getImageDescriptor(ISharedImages.IMG_OBJ_FOLDER));
	}

	protected IOverlayDescriptorFactory fDescriptorFactory;
	protected Point fIconSize;
	protected String fSizeKey;
	
	public OverlayIconManager(IOverlayDescriptorFactory descriptorFactory, Point iconSize) {
		fDescriptorFactory= descriptorFactory;
		fIconSize= iconSize;
		fSizeKey= fIconSize.x+"/"+fIconSize.y; //$NON-NLS-1$
	}
	
	/**
	 * @deprecated use OverlayIconManager(IOverlayDescriptorFactory descriptorFactory, Point iconSize) instead.
	 */
	public OverlayIconManager(IOverlayDescriptorFactory descriptorFactory) {
		this(descriptorFactory, JavaImageLabelProvider.BIG_SIZE);
	}

	/**
	 * Returns an icon adorned based on the properties of the source reference.
	 */
	public Image getIcon(String baseKey, Object element) {
		IOverlayDescriptor key= fDescriptorFactory.createDescriptor(baseKey, element);
		ImageRegistry registry= JavaPlugin.getDefault().getImageRegistry();
		Image image= get(registry, key);
		if (image == null) {
			image= (new OverlayIcon(getBaseImageDescriptor(baseKey), key.getOverlays(), fIconSize)).createImage();
			put(registry, key, image);
		}
		return image;
	}

	private Image get(ImageRegistry registry, IOverlayDescriptor key) {
		return registry.get(key.getKey()+fSizeKey);
	}
	
	private void put(ImageRegistry registry, IOverlayDescriptor key, Image icon) {
		registry.put(key.getKey()+fSizeKey, icon);
	}
	
	private static ImageDescriptor getBaseImageDescriptor(String key) {
		return (ImageDescriptor)fgBaseImageDescriptors.get(key);
	}
	
	// fix: 1GF6CDH: ITPJUI:ALL - Packages view doesn't show the nature decoration
	public static void manage(String key, ImageDescriptor descriptor) {
		fgBaseImageDescriptors.put(key, descriptor);
	}
	
	public static boolean isManaged(String key) {
		return fgBaseImageDescriptors.containsKey(key);
	}
}