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
package org.eclipse.jdt.internal.ui.javaeditor;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.jface.text.source.Annotation;

import org.eclipse.ui.texteditor.IAnnotationImageProvider;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.correction.JavaCorrectionProcessor;

/**
 * Image provider for annotations based on Java problem markers.
 *
 * @since 3.0
 */
public class JavaAnnotationImageProvider implements IAnnotationImageProvider {

	private final static int NO_IMAGE= 0;
	private final static int GRAY_IMAGE= 1;
	private final static int OVERLAY_IMAGE= 2;
	private final static int QUICKFIX_WARNING_IMAGE= 3;
	private final static int QUICKFIX_ERROR_IMAGE= 4;
	private final static int QUICKFIX_INFO_IMAGE= 5;


	private static Image fgQuickFixWarningImage;
	private static Image fgQuickFixErrorImage;
	private static Image fgQuickFixInfoImage;
	private boolean fShowQuickFixIcon;
	private int fCachedImageType;
	private Image fCachedImage;


	public JavaAnnotationImageProvider() {
		fShowQuickFixIcon= PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.EDITOR_CORRECTION_INDICATION);
	}

	/*
	 * @see org.eclipse.jface.text.source.IAnnotationImageProvider#getManagedImage(org.eclipse.jface.text.source.Annotation)
	 */
	@Override
	public Image getManagedImage(Annotation annotation) {
		if (annotation instanceof IJavaAnnotation) {
			IJavaAnnotation javaAnnotation= (IJavaAnnotation) annotation;
			int imageType= getImageType(javaAnnotation);
			return getImage(javaAnnotation, imageType);
		}
		return null;
	}

	/*
	 * @see org.eclipse.jface.text.source.IAnnotationImageProvider#getImageDescriptorId(org.eclipse.jface.text.source.Annotation)
	 */
	@Override
	public String getImageDescriptorId(Annotation annotation) {
		// unmanaged images are not supported
		return null;
	}

	/*
	 * @see org.eclipse.jface.text.source.IAnnotationImageProvider#getImageDescriptor(java.lang.String)
	 */
	@Override
	public ImageDescriptor getImageDescriptor(String symbolicName) {
		// unmanaged images are not supported
		return null;
	}


	private boolean showQuickFix(IJavaAnnotation annotation) {
		return fShowQuickFixIcon && annotation.isProblem() && JavaCorrectionProcessor.hasCorrections((Annotation) annotation);
	}

	private Image getQuickFixWarningImage() {
		if (fgQuickFixWarningImage == null)
			fgQuickFixWarningImage= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_FIXABLE_WARNING);
		return fgQuickFixWarningImage;
	}

	private Image getQuickFixErrorImage() {
		if (fgQuickFixErrorImage == null)
			fgQuickFixErrorImage= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_FIXABLE_ERROR);
		return fgQuickFixErrorImage;
	}

	private Image getQuickFixInfoImage() {
		if (fgQuickFixInfoImage == null)
			fgQuickFixInfoImage= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_FIXABLE_INFO);
		return fgQuickFixInfoImage;
	}

	private int getImageType(IJavaAnnotation annotation) {
		int imageType= NO_IMAGE;
		if (annotation.hasOverlay())
			imageType= OVERLAY_IMAGE;
		else if (!annotation.isMarkedDeleted()) {
			if (showQuickFix(annotation)) {
				boolean nomatch= false;
				if (annotation.getType() != null) switch (annotation.getType()) {
				case JavaMarkerAnnotation.ERROR_ANNOTATION_TYPE:
					imageType= QUICKFIX_ERROR_IMAGE;
					break;
				case JavaMarkerAnnotation.WARNING_ANNOTATION_TYPE:
					imageType= QUICKFIX_WARNING_IMAGE;
					break;
				default:
					nomatch= true;
					break;
				}
				if (nomatch) {
					imageType= QUICKFIX_INFO_IMAGE;
				}
			}
		} else {
			imageType= GRAY_IMAGE;
		}
		return imageType;
	}

	private Image getImage(IJavaAnnotation annotation, int imageType) {
		if ((imageType == QUICKFIX_WARNING_IMAGE || imageType == QUICKFIX_ERROR_IMAGE || imageType == QUICKFIX_INFO_IMAGE) && fCachedImageType == imageType)
			return fCachedImage;

		if (Display.getCurrent() == null) {
			return null;
		}

		Image image= null;
		switch (imageType) {
			case OVERLAY_IMAGE:
				IJavaAnnotation overlay= annotation.getOverlay();
				image= getManagedImage((Annotation) overlay);
				fCachedImageType= -1;
				break;
			case QUICKFIX_WARNING_IMAGE:
				image= getQuickFixWarningImage();
				fCachedImageType= imageType;
				fCachedImage= image;
				break;
			case QUICKFIX_ERROR_IMAGE:
				image= getQuickFixErrorImage();
				fCachedImageType= imageType;
				fCachedImage= image;
				break;
			case QUICKFIX_INFO_IMAGE:
				image= getQuickFixInfoImage();
				fCachedImageType= imageType;
				fCachedImage= image;
				break;
			case GRAY_IMAGE: {
				String annotationType= annotation.getType();
				if (null != annotationType) switch (annotationType) {
					case JavaMarkerAnnotation.ERROR_ANNOTATION_TYPE:
						image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_ERROR_ALT);
						break;
					case JavaMarkerAnnotation.WARNING_ANNOTATION_TYPE:
						image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_WARNING_ALT);
						break;
					case JavaMarkerAnnotation.INFO_ANNOTATION_TYPE:
						image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_INFO_ALT);
						break;
					default:
						break;
				}
				fCachedImageType= -1;
				break;
			}
		}

		return image;
	}
}
