/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.viewsupport;


import org.eclipse.jface.resource.ImageDescriptor;

/**
 * An overlay descriptor identifies (getKey()) and creates (getOverlays()) icon
 * overlays. IOverlayDescriptors are created by a IOverlayDescriptorFactory. The
 * IOverlayDescriptor knows about the meaning of the icons it shows. For example,
 * there is an overlay descriptor for java flags, knowing about public, static, etc.
 * Still, the overlay descriptors do not know about what elements they will be used
 * for. We use the IOverlayDescriptorFactory to get a IOverlayDescriptor for, say, 
 * a ISourceReference or a debug variable.
 */
public interface IOverlayDescriptor {
	/**
	 * Returns the name that serves as a key to retrieve the base of the overlay.
	 */
	String getBaseName();
	
	/**
	 * Return the arrays of overlay icons. This method will be called once for 
	 * each distinct overlay icon.
	 * The icons will be placed as follows (based on the index in the array of arrays)
	 * array[0][x]: place top right corner
	 * array[1][x]: place in bottom right corner
	 * array[2][x]: place in bottom left corner
	 * array[3][x]: place in top left corner
	 * a maximum of 3 icons will be used per array
	 * you may pass shorter arrays, but if you want to use the 3rd array, you'll have
	 * to pass an array of arrays that's three elements long, null out the first two entries.
	 */
	ImageDescriptor[][] getOverlays();
	
	/**
	 * get a unique String identifying this overlay descriptor. If two descriptors
	 * return the same key, they are considered to describe the same overlay icon.
	 */
	String getKey();
}