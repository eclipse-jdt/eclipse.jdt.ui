/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
package org.eclipse.jdt.ui;


import org.eclipse.swt.graphics.Point;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;

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
 *
 * @noextend This class is not intended to be subclassed by clients.
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

	/**
	 * Flag to render the 'volatile' adornment.
	 * @since 3.3
	 */
	public final static int VOLATILE= 	0x800;

	/**
	 * Flag to render the 'transient' adornment.
	 * @since 3.3
	 */
	public final static int TRANSIENT= 	0x1000;

	/**
	 * Flag to render the build path error adornment.
	 * @since 3.7
	 */
	public final static int BUILDPATH_ERROR= 0x2000;

	/**
	 * Flag to render the 'native' adornment.
	 * @since 3.7
	 */
	public final static int NATIVE= 	0x4000;

	/**
	 * Flag to render the 'ignore optional compile problems' adornment.
	 * @since 3.8
	 */
	public final static int IGNORE_OPTIONAL_PROBLEMS= 0x8000;

	/**
	 * Flag to render the 'default' method adornment.
	 *
	 * @since 3.10
	 */
	public final static int DEFAULT_METHOD= 0x10000;

	/**
	 * Flag to render the 'default' annotation adornment.
	 *
	 * @since 3.10
	 */
	public final static int ANNOTATION_DEFAULT= 0x20000;

	/**
	 * Flag to render the info adornment.
	 *
	 * @since 3.12
	 */
	public final static int INFO= 0x40000;

	/** Flag to render the sealed adornment.
	 *
	 * @noreference This field is not intended to be referenced by clients.
	 * @since 3.22
	 */
	public final static int SEALED=	0x10000000;

	/** Flag to render the sealed adornment.
	 *
	 * @noreference This field is not intended to be referenced by clients.
	 * @since 3.22
	 */
	public final static int NON_SEALED=	0x4000000;

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
	 * {@link #SYNCHRONIZED}, {@link #STATIC}, {@link #RUNNABLE}, {@link #INFO}, {@link #WARNING},
	 * {@link #ERROR}, {@link #OVERRIDES}, {@link #IMPLEMENTS}, {@link #CONSTRUCTOR},
	 * {@link #DEPRECATED}, {@link #VOLATILE}, {@link #TRANSIENT}, {@link #BUILDPATH_ERROR},
	 * {@link #NATIVE}, or any combination of those.
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

	@Override
	protected Point getSize() {
		return fSize;
	}

	@Override
	public boolean equals(Object object) {
		if (object == null || !JavaElementImageDescriptor.class.equals(object.getClass()))
			return false;

		JavaElementImageDescriptor other= (JavaElementImageDescriptor)object;
		return (fBaseImage.equals(other.fBaseImage) && fFlags == other.fFlags && fSize.equals(other.fSize));
	}

	@Override
	public int hashCode() {
		return fBaseImage.hashCode() | fFlags | fSize.hashCode();
	}

	@Override
	protected void drawCompositeImage(int width, int height) {
		CachedImageDataProvider bg= createCachedImageDataProvider(fBaseImage);

		if ((fFlags & DEPRECATED) != 0) { // draw *behind* the full image
			Point size= getSize();
			CachedImageDataProvider deprecatedProvider= createCachedImageDataProvider(JavaPluginImages.DESC_OVR_DEPRECATED);
			drawImage(deprecatedProvider, 0, size.y - deprecatedProvider.getHeight());
		}
		drawImage(bg, 0, 0);

		drawTopRight();
		drawBottomRight();
		drawBottomLeft();
	}

	private void addTopRightImage(ImageDescriptor desc, Point pos) {
		CachedImageDataProvider provider= createCachedImageDataProvider(desc);
		int x= pos.x - provider.getWidth();
		if (x >= 0) {
			drawImage(provider, x, pos.y);
			pos.x= x;
		}
	}

	private void addBottomRightImage(ImageDescriptor desc, Point pos) {
		CachedImageDataProvider provider= createCachedImageDataProvider(desc);
		int x= pos.x - provider.getWidth();
		int y= pos.y - provider.getHeight();
		if (x >= 0 && y >= 0) {
			drawImage(provider, x, y);
			pos.x= x;
		}
	}

	private void addBottomLeftImage(ImageDescriptor desc, Point pos) {
		CachedImageDataProvider provider= createCachedImageDataProvider(desc);
		int x= pos.x;
		int y= pos.y - provider.getHeight();
		int x2= x + provider.getWidth();
		if (x2 < getSize().x && y >= 0) {
			drawImage(provider, x, y);
			pos.x= x2;
		}
	}


	private void drawTopRight() {
		Point pos= new Point(getSize().x, 0);
		if ((fFlags & ABSTRACT) != 0) {
			addTopRightImage(JavaPluginImages.DESC_OVR_ABSTRACT, pos);
		}
		if ((fFlags & CONSTRUCTOR) != 0) {
			addTopRightImage(JavaPluginImages.DESC_OVR_CONSTRUCTOR, pos);
		}
		if ((fFlags & FINAL) != 0) {
			addTopRightImage(JavaPluginImages.DESC_OVR_FINAL, pos);
		}
		if ((fFlags & VOLATILE) != 0) {
			addTopRightImage(JavaPluginImages.DESC_OVR_VOLATILE, pos);
		}
		if ((fFlags & STATIC) != 0) {
			addTopRightImage(JavaPluginImages.DESC_OVR_STATIC, pos);
		}
		if ((fFlags & NATIVE) != 0) {
			addTopRightImage(JavaPluginImages.DESC_OVR_NATIVE, pos);
		}
		if ((fFlags & DEFAULT_METHOD) != 0) {
			addTopRightImage(JavaPluginImages.DESC_OVR_ANNOTATION_DEFAULT_METHOD, pos);
		}
		if ((fFlags & ANNOTATION_DEFAULT) != 0) {
			addTopRightImage(JavaPluginImages.DESC_OVR_ANNOTATION_DEFAULT_METHOD, pos);
		}
		if ((fFlags & SEALED) != 0) {
			addTopRightImage(JavaPluginImages.DESC_OVR_SEALED, pos);
		}
		if ((fFlags & NON_SEALED) != 0) {
			addTopRightImage(JavaPluginImages.DESC_OVR_NON_SEALED, pos);
		}
	}

	private void drawBottomRight() {
		Point size= getSize();
		Point pos= new Point(size.x, size.y);

		int flags= fFlags;

		int syncAndOver= SYNCHRONIZED | OVERRIDES;
		int syncAndImpl= SYNCHRONIZED | IMPLEMENTS;

		// methods:
		if ((flags & syncAndOver) == syncAndOver) { // both flags set: merged overlay image
			addBottomRightImage(JavaPluginImages.DESC_OVR_SYNCH_AND_OVERRIDES, pos);
			flags &= ~syncAndOver; // clear to not render again
		} else if ((flags & syncAndImpl) == syncAndImpl) { // both flags set: merged overlay image
			addBottomRightImage(JavaPluginImages.DESC_OVR_SYNCH_AND_IMPLEMENTS, pos);
			flags &= ~syncAndImpl; // clear to not render again
		}
		if ((flags & OVERRIDES) != 0) {
			addBottomRightImage(JavaPluginImages.DESC_OVR_OVERRIDES, pos);
		}
		if ((flags & IMPLEMENTS) != 0) {
			addBottomRightImage(JavaPluginImages.DESC_OVR_IMPLEMENTS, pos);
		}
		if ((flags & SYNCHRONIZED) != 0) {
			addBottomRightImage(JavaPluginImages.DESC_OVR_SYNCH, pos);
		}

		// types:
		if ((flags & RUNNABLE) != 0) {
			addBottomRightImage(JavaPluginImages.DESC_OVR_RUN, pos);
		}

		// fields:
		if ((flags & TRANSIENT) != 0) {
			addBottomRightImage(JavaPluginImages.DESC_OVR_TRANSIENT, pos);
		}
	}

	private void drawBottomLeft() {
		Point pos= new Point(0, getSize().y);
		if ((fFlags & ERROR) != 0) {
			addBottomLeftImage(JavaPluginImages.DESC_OVR_ERROR, pos);
		}
		if ((fFlags & BUILDPATH_ERROR) != 0) {
			addBottomLeftImage(JavaPluginImages.DESC_OVR_BUILDPATH_ERROR, pos);
		}
		if ((fFlags & WARNING) != 0) {
			addBottomLeftImage(JavaPluginImages.DESC_OVR_WARNING, pos);
		}
		if ((fFlags & IGNORE_OPTIONAL_PROBLEMS) != 0) {
			addBottomLeftImage(JavaPluginImages.DESC_OVR_IGNORE_OPTIONAL_PROBLEMS, pos);
		}
		if ((fFlags & INFO) != 0) {
			addBottomLeftImage(JavaPluginImages.DESC_OVR_INFO, pos);
		}
	}
}
