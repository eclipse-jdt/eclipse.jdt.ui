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

package org.eclipse.jdt.internal.ui.examples.jspeditor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.source.Annotation;

import org.eclipse.ui.texteditor.MarkerAnnotation;

import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.javaeditor.AnnotationType;
import org.eclipse.jdt.internal.ui.javaeditor.IJavaAnnotation;
import org.eclipse.jdt.internal.ui.text.correction.JavaCorrectionProcessor;

/**
 * Temporary annotations.
 * <p>
 * Note: Copied from CompilationUnitDocumentProvider-CompilationUnitAnnotationModel and modified.
 * </p>
 *  
 * @see org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitDocumentProvider
 * @since 3.0
 */
public class TemporaryAnnotation extends Annotation implements IAnnotationExtension {

	public final static int NONE= 0;
	public final static int WARNING= 1;
	public final static int ERROR= 2;
		
	private static Image fgQuickFixImage;
	private static Image fgQuickFixErrorImage;
	private static boolean fgQuickFixImagesInitialized= false;
	
	private List fOverlaids;
	private Image fImage;
	private boolean fQuickFixImagesInitialized= false;
	private AnnotationType fType;
	private int fSeverity;
	private int fId;
	private String fMessage;
	
	
	public TemporaryAnnotation(int type, String message, int id) {
		Assert.isTrue(type == NONE || type == WARNING || type == ERROR);
		fSeverity= type;
		fId= id;
		fMessage= message;
		setLayer(MarkerAnnotation.PROBLEM_LAYER + 1);
		
		if (IProblem.Task == id)
			fType= AnnotationType.TASK;
		else if (isWarning())
			fType= AnnotationType.WARNING;
		else
			fType= AnnotationType.ERROR;			
	}
	
	private void initializeImages() {
		// http://bugs.eclipse.org/bugs/show_bug.cgi?id=18936
		if (!fQuickFixImagesInitialized) {
			if (indicateQuixFixableProblems() && JavaCorrectionProcessor.hasCorrections(fId)) {
				if (!fgQuickFixImagesInitialized) {
					fgQuickFixImage= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_FIXABLE_PROBLEM);
					fgQuickFixErrorImage= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_FIXABLE_ERROR);
					fgQuickFixImagesInitialized= true;
				}
				if (fType == AnnotationType.ERROR)
					fImage= fgQuickFixErrorImage;
				else
					fImage= fgQuickFixImage;
			}
			fQuickFixImagesInitialized= true;
		}
	}

	private boolean indicateQuixFixableProblems() {
		return PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.EDITOR_CORRECTION_INDICATION);
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
	
	public boolean isWarning() {
		return  fSeverity == WARNING;
	}

	public boolean isError()  {
		return fSeverity == ERROR;
	}
	
	/*
	 * @see IJavaAnnotation#isRelevant()
	 */
	public boolean isRelevant() {
		return true;
	}
	
	/*
	 * @see IJavaAnnotation#hasOverlay()
	 */
	public boolean hasOverlay() {
		return false;
	}
	
	/*
	 * @see IJavaAnnotation#addOverlaid(IJavaAnnotation)
	 */
	public void addOverlaid(IJavaAnnotation annotation) {
		if (fOverlaids == null)
			fOverlaids= new ArrayList(1);
		fOverlaids.add(annotation);
	}

	/*
	 * @see IJavaAnnotation#removeOverlaid(IJavaAnnotation)
	 */
	public void removeOverlaid(IJavaAnnotation annotation) {
		if (fOverlaids != null) {
			fOverlaids.remove(annotation);
			if (fOverlaids.size() == 0)
				fOverlaids= null;
		}
	}
	
	/*
	 * @see IAnnotationExtension#getMessage()
	 */
	public String getMessage() {
		return fMessage;
	}

	/*
	 * @see IAnnotationExtension#getId()
	 */
	public int getId() {
		return fId;
	}

	/*
	 * @see IAnnotationExtension#getType()
	 */
	public Object getType() {
		// XXX: This is currently a hack to bring the marker-based demo to live 
		return new Integer(fSeverity);
	}

	
	/*
	 * @see IAnnotationExtension#isTemporary()
	 */
	public boolean isTemporary() {
		return true;
	}
}
