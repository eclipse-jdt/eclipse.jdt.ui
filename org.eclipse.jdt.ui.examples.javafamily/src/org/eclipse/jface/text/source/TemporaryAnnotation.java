/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jface.text.source;

import java.util.Iterator;

import org.eclipse.core.resources.IMarker;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.text.Assert;

import org.eclipse.ui.texteditor.AnnotationPreference;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.ui.texteditor.MarkerAnnotationPreferences;

/**
 * Temporary annotation.
 * <p>
 * Note: Copied from CompilationUnitDocumentProvider-CompilationUnitAnnotationModel and modified.</p>
 * <p>
 * XXX: Currently we only show images for temporary annotations if
 *		there is a quick fix. A simple temporary annotation cannot
 *		decide if there is a quick fix therefore no image will be
 *		returned for this temporary annotation.</p>
 * 
 * @see org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitDocumentProvider
 * @since 3.0
 */
public class TemporaryAnnotation extends Annotation implements IAnnotationExtension {

	public final static int NONE= 0;
	public final static int WARNING= 1;
	public final static int ERROR= 2;
		
//	private static Image fgWarningImage;
//	private static Image fgErrorImage;
//	private static boolean fgImagesInitialized= false;
	
	private Image fImage;
	private boolean fImageInitialized= false;
	private int fSeverity;
	private String fMessage;

	/** The marker annotation preferences */
	private MarkerAnnotationPreferences fMarkerAnnotationPreferences;
	
	
	public TemporaryAnnotation(int severity, String message) {
		Assert.isTrue(severity == NONE || severity == WARNING || severity == ERROR);
		fSeverity= severity;
		fMessage= message;
		setLayer(MarkerAnnotation.PROBLEM_LAYER + 1);
		fMarkerAnnotationPreferences= new MarkerAnnotationPreferences();
	}
	
	private void initializeImages() {
		if (!fImageInitialized) {

		/*
		 * XXX: currently we only show images for temporary annotations if
		 *		there is a quick fix. A simple temporary annotation cannot
		 *		decide if there is a quick fix.
		 */

//			if (!fgImagesInitialized) {
//				fgWarningImage= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_FIXABLE_PROBLEM);
//				fgErrorImage= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_FIXABLE_ERROR);
//				fgImagesInitialized= true;
//			}
//
//			if (fSeverity == ERROR)
//				fImage= fgErrorImage;
//			else
//				fImage= fgWarningImage;
//
			fImageInitialized= true;
		}
	}

	/*
	 * @see Annotation#paint
	 */
	public void paint(GC gc, Canvas canvas, Rectangle r) {
		initializeImages();
		if (fImage != null)
			drawImage(fImage, gc, canvas, r, SWT.CENTER, SWT.TOP);
	}
	
	/*
	 * @see IJavaAnnotation#getImage(Display)
	 */
	public Image getImage(Display display) {
		initializeImages();
		return fImage;
	}
	
	/*
	 * @see IAnnotationExtension#getMessage()
	 */
	public String getMessage() {
		return fMessage;
	}

	/*
	 * @see IAnnotationExtension#getType()
	 */
	public Object getType() {
		AnnotationPreference pref= getAnnotationPreference();
		if (pref != null)
			return pref.getAnnotationType();
		else
			return null;
	}

	/**
	 * Returns the annotation preference for this annotation.
	 * 
	 * @return the annotation preference or <code>null</code> if none
	 */	
	private AnnotationPreference getAnnotationPreference() {
		Iterator e= fMarkerAnnotationPreferences.getAnnotationPreferences().iterator();
		while (e.hasNext()) {
			AnnotationPreference info= (AnnotationPreference) e.next();
			if (info.getMarkerType() == IMarker.PROBLEM && fSeverity == info.getSeverity())
				return info;
		}
			
		return null;
	}
	
	/*
	 * @see IAnnotationExtension#isTemporary()
	 */
	public boolean isTemporary() {
		return true;
	}
}
