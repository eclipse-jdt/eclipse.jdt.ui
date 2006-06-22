/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui;


import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.Assert;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * A {@link JavaElementImageDescriptor} consists of a base image and several adornments. The adornments
 * are computed according to the flags either passed during creation or set via the method
 *{@link #setAdornments(int)}. 
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 *
 * @since 2.0 
 */
public class JavaElementImageDescriptor extends CompositeImageDescriptor {
	
	/** Flag to render the abstract adornment. */
	public final static int ABSTRACT= 		0x001;
	
	/** Flag to render the final adornment. */
	public final static int FINAL=			0x002;
	
	/** Flag to render the synchronized adornment. */
	public final static int SYNCHRONIZED=	0x004;
	
	/** Flag to render the static adornment. */
	public final static int STATIC=			0x008;
	
	/** Flag to render the runnable adornment. */
	public final static int RUNNABLE= 		0x010;
	
	/** Flag to render the warning adornment. */
	public final static int WARNING=			0x020;
	
	/** Flag to render the error adornment. */
	public final static int ERROR=			0x040;
	
	/** Flag to render the 'override' adornment. */
	public final static int OVERRIDES= 		0x080;
	
	/** Flag to render the 'implements' adornment. */
	public final static int IMPLEMENTS= 		0x100;
	
	/** Flag to render the 'constructor' adornment. */
	public final static int CONSTRUCTOR= 	0x200;
	
	/**
	 * Flag to render the 'deprecated' adornment.
	 * @since 3.0
	 */
	public final static int DEPRECATED= 	0x400;	

	private ImageDescriptor fBaseImage;
	private int fFlags;
	private Point fSize;

	/**
	 * Creates a new JavaElementImageDescriptor.
	 * 
	 * @param baseImage an image descriptor used as the base image
	 * @param flags flags indicating which adornments are to be rendered. See {@link #setAdornments(int)}
	 * 	for valid values.
	 * @param size the size of the resulting image
	 */
	public JavaElementImageDescriptor(ImageDescriptor baseImage, int flags, Point size) {
		fBaseImage= baseImage;
		Assert.isNotNull(fBaseImage);
		fFlags= flags;
		Assert.isTrue(fFlags >= 0);
		fSize= size;
		Assert.isNotNull(fSize);
	}
	
	/**
	 * Sets the descriptors adornments. Valid values are: {@link #ABSTRACT}, {@link #FINAL},
	 * {@link #SYNCHRONIZED}, {@link #STATIC}, {@link #RUNNABLE}, {@link #WARNING}, 
	 * {@link #ERROR}, {@link #OVERRIDES}, {@link #IMPLEMENTS}, {@link #CONSTRUCTOR},
	 * {@link #DEPRECATED},  or any combination of those.
	 * 
	 * @param adornments the image descriptors adornments
	 */
	public void setAdornments(int adornments) {
		Assert.isTrue(adornments >= 0);
		fFlags= adornments;
	}

	/**
	 * Returns the current adornments.
	 * 
	 * @return the current adornments
	 */
	public int getAdronments() {
		return fFlags;
	}

	/**
	 * Sets the size of the image created by calling {@link #createImage()}.
	 * 
	 * @param size the size of the image returned from calling {@link #createImage()}
	 */
	public void setImageSize(Point size) {
		Assert.isNotNull(size);
		Assert.isTrue(size.x >= 0 && size.y >= 0);
		fSize= size;
	}
	
	/**
	 * Returns the size of the image created by calling {@link #createImage()}.
	 * 
	 * @return the size of the image created by calling {@link #createImage()}
	 */
	public Point getImageSize() {
		return new Point(fSize.x, fSize.y);
	}
	
	/* (non-Javadoc)
	 * Method declared in CompositeImageDescriptor
	 */
	protected Point getSize() {
		return fSize;
	}
	
	/* (non-Javadoc)
	 * Method declared on Object.
	 */
	public boolean equals(Object object) {
		if (object == null || !JavaElementImageDescriptor.class.equals(object.getClass()))
			return false;
			
		JavaElementImageDescriptor other= (JavaElementImageDescriptor)object;
		return (fBaseImage.equals(other.fBaseImage) && fFlags == other.fFlags && fSize.equals(other.fSize));
	}
	
	/* (non-Javadoc)
	 * Method declared on Object.
	 */
	public int hashCode() {
		return fBaseImage.hashCode() | fFlags | fSize.hashCode();
	}
	
	/* (non-Javadoc)
	 * Method declared in CompositeImageDescriptor
	 */
	protected void drawCompositeImage(int width, int height) {
		ImageData bg= getImageData(fBaseImage);
			
		
		if ((fFlags & DEPRECATED) != 0) { // over the full image
			Point size= getSize();
			ImageData data= getImageData(JavaPluginImages.DESC_OVR_DEPRECATED);
			drawImage(data, 0, size.y - data.height);
		}
		drawImage(bg, 0, 0);
				
		drawTopRight();
		drawBottomRight();
		drawBottomLeft();
		

	}
	
	private ImageData getImageData(ImageDescriptor descriptor) {
		ImageData data= descriptor.getImageData(); // see bug 51965: getImageData can return null
		if (data == null) {
			data= DEFAULT_IMAGE_DATA;
			JavaPlugin.logErrorMessage("Image data not available: " + descriptor.toString()); //$NON-NLS-1$
		}
		return data;
	}
	
	
	private void drawTopRight() {		
		int x= getSize().x;
		if ((fFlags & ABSTRACT) != 0) {
			ImageData data= getImageData(JavaPluginImages.DESC_OVR_ABSTRACT);
			x-= data.width;
			drawImage(data, x, 0);
		}
		if ((fFlags & CONSTRUCTOR) != 0) {
			ImageData data= getImageData(JavaPluginImages.DESC_OVR_CONSTRUCTOR);
			x-= data.width;
			drawImage(data, x, 0);
		}
		if ((fFlags & FINAL) != 0) {
			ImageData data= getImageData(JavaPluginImages.DESC_OVR_FINAL);
			x-= data.width;
			drawImage(data, x, 0);
		}
		if ((fFlags & STATIC) != 0) {
			ImageData data= getImageData(JavaPluginImages.DESC_OVR_STATIC);
			x-= data.width;
			drawImage(data, x, 0);
		}
	}		
	
	private void drawBottomRight() {
		Point size= getSize();
		int x= size.x;
		int flags= fFlags;
		
		int syncAndOver= SYNCHRONIZED | OVERRIDES;
		int syncAndImpl= SYNCHRONIZED | IMPLEMENTS;
		
		if ((flags & syncAndOver) == syncAndOver) { // both flags set: merged overlay image
			ImageData data= getImageData(JavaPluginImages.DESC_OVR_SYNCH_AND_OVERRIDES);
			x-= data.width;
			drawImage(data, x, size.y - data.height);
			flags &= ~syncAndOver; // clear to not render again
		} else if ((flags & syncAndImpl) == syncAndImpl) { // both flags set: merged overlay image
			ImageData data= getImageData(JavaPluginImages.DESC_OVR_SYNCH_AND_IMPLEMENTS);
			x-= data.width;
			drawImage(data, x, size.y - data.height);
			flags &= ~syncAndImpl; // clear to not render again
		}
		if ((flags & OVERRIDES) != 0) {
			ImageData data= getImageData(JavaPluginImages.DESC_OVR_OVERRIDES);
			x-= data.width;
			drawImage(data, x, size.y - data.height);
		}
		if ((flags & IMPLEMENTS) != 0) {
			ImageData data= getImageData(JavaPluginImages.DESC_OVR_IMPLEMENTS);
			x-= data.width;
			drawImage(data, x, size.y - data.height);
		}
		if ((flags & SYNCHRONIZED) != 0) {
			ImageData data= getImageData(JavaPluginImages.DESC_OVR_SYNCH);
			x-= data.width;
			drawImage(data, x, size.y - data.height);
		}
		if ((flags & RUNNABLE) != 0) {
			ImageData data= getImageData(JavaPluginImages.DESC_OVR_RUN);
			x-= data.width;
			drawImage(data, x, size.y - data.height);
		}
	}		
	
	private void drawBottomLeft() {
		Point size= getSize();
		int x= 0;
		if ((fFlags & ERROR) != 0) {
			ImageData data= getImageData(JavaPluginImages.DESC_OVR_ERROR);
			drawImage(data, x, size.y - data.height);
			x+= data.width;
		}
		if ((fFlags & WARNING) != 0) {
			ImageData data= getImageData(JavaPluginImages.DESC_OVR_WARNING);
			drawImage(data, x, size.y - data.height);
			x+= data.width;
		}

	}		
}
