/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.viewsupport;


import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.Assert;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * A JavaImageDescriptor consists of a main icon and several adornments. The adornments
 * are computed according to Java element's modifiers (e.g. visibility, static, final, ...). 
 */
public class JavaElementImageDescriptor extends CompositeImageDescriptor {
	
	/** Flag to render the abstract adornment */
	public final static int ABSTRACT= 		0x001;
	/** Flag to render the final adornment */
	public final static int FINAL=			0x002;
	/** Flag to render the synchronized adornment */
	public final static int SYNCHRONIZED=	0x004;
	/** Flag to render the static adornment */
	public final static int STATIC=			0x008;
	/** Flag to render the runnable adornment */
	public final static int RUNNABLE= 		0x010;
	/** Flag to render the waring adornment */
	public final static int WARNING=		0x020;
	/** Flag to render the error adornment */
	public final static int ERROR=			0x040;
	/** Flag to render the error adornment */
	public final static int OVERRIDDEN= 0x080;	
	
	private ImageDescriptor fBaseImage;
	private int fFlags;
	private Point fSize;
	
	/**
	 * Create a new JavaElementImageDescriptor.
	 * 
	 * @param baseImage an image descriptor used as the base image
	 * @param flags flags indicating which adornments are to be rendered
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
		if (!JavaElementImageDescriptor.class.equals(object.getClass()))
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
		ImageData bg;
		if ((bg= fBaseImage.getImageData()) == null)
			bg= DEFAULT_IMAGE_DATA;
			
		drawImage(bg, 0, 0);
		drawTopRight();
		drawBottomRight();
		drawBottomLeft();
	}	
	
	private void drawTopRight() {		
		int x= getSize().x;
		ImageData data= null;
		if ((fFlags & ABSTRACT) != 0) {
			data= JavaPluginImages.DESC_OVR_ABSTRACT.getImageData();
			x-= data.width;
			drawImage(data, x, 0);
		}
		if ((fFlags & FINAL) != 0) {
			data= JavaPluginImages.DESC_OVR_FINAL.getImageData();
			x-= data.width;
			drawImage(data, x, 0);
		}
		if ((fFlags & STATIC) != 0) {
			data= JavaPluginImages.DESC_OVR_STATIC.getImageData();
			x-= data.width;
			drawImage(data, x, 0);
		}
	}		
	
	private void drawBottomRight() {
		Point size= getSize();
		int x= size.x;
		ImageData data= null;
		if ((fFlags & SYNCHRONIZED) != 0) {
			data= JavaPluginImages.DESC_OVR_SYNCH.getImageData();
			x-= data.width;
			drawImage(data, x, size.y - data.height);
		}
		if ((fFlags & RUNNABLE) != 0) {
			data= JavaPluginImages.DESC_OVR_RUN.getImageData();
			x-= data.width;
			drawImage(data, x, size.y - data.height);
		}
		if ((fFlags & OVERRIDDEN) != 0) {
			data= JavaPluginImages.DESC_OVR_OVERRIDDEN.getImageData();
			x-= data.width;
			drawImage(data, x, size.y - data.height);
		}		
	}		
	
	private void drawBottomLeft() {
		Point size= getSize();
		int x= 0;
		ImageData data= null;
		if ((fFlags & ERROR) != 0) {
			data= JavaPluginImages.DESC_OVR_ERROR.getImageData();
			drawImage(data, x, size.y - data.height);
			x+= data.width;
		}
		if ((fFlags & WARNING) != 0) {
			data= JavaPluginImages.DESC_OVR_WARNING.getImageData();
			drawImage(data, x, size.y - data.height);
			x+= data.width;
		}
	}		
}