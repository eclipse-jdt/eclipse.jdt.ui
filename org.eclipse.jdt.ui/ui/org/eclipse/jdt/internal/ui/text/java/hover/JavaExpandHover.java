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
package org.eclipse.jdt.internal.ui.text.java.hover;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.viewers.IDoubleClickListener;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControlExtension2;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.IAnnotationAccess;
import org.eclipse.jface.text.source.IAnnotationAccessExtension;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationPresentation;
import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.ui.texteditor.AnnotationPreference;
import org.eclipse.ui.texteditor.AnnotationPreferenceLookup;

import org.eclipse.ui.internal.texteditor.AnnotationExpandHover;
import org.eclipse.ui.internal.texteditor.AnnotationExpansionControl;
import org.eclipse.ui.internal.texteditor.AnnotationExpansionControl.AnnotationHoverInput;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.javaeditor.IJavaAnnotation;
import org.eclipse.jdt.internal.ui.javaeditor.JavaMarkerAnnotation;

/**
 * 
 * 
 * @since 3.0
 */
public class JavaExpandHover extends AnnotationExpandHover {
	
	/** Id of the no breakpoint fake annotation */
	public static final String NO_BREAKPOINT_ANNOTATION= "org.eclipse.jdt.internal.ui.NoBreakpointAnnotation"; //$NON-NLS-1$

	private static class NoBreakpointAnnotation extends Annotation implements IAnnotationPresentation {
		
		public NoBreakpointAnnotation() {
			super(NO_BREAKPOINT_ANNOTATION, false, "Add a breakpoint");
		}
		
		/*
		 * @see org.eclipse.jface.text.source.Annotation#paint(org.eclipse.swt.graphics.GC, org.eclipse.swt.widgets.Canvas, org.eclipse.swt.graphics.Rectangle)
		 */
		public void paint(GC gc, Canvas canvas, Rectangle bounds) {
			// draw affordance so the user know she can click here to get a breakpoint
			Image fImage= JavaPluginImages.get(JavaPluginImages.IMG_FIELD_PUBLIC);
			drawImage(fImage, gc, canvas, bounds, SWT.CENTER);
		}
	}
	
	private AnnotationPreferenceLookup fLookup= new AnnotationPreferenceLookup();
	
	public JavaExpandHover(CompositeRuler ruler, IAnnotationAccess access, IDoubleClickListener doubleClickListener) {
		super(ruler, access, doubleClickListener);
	}
	
	/*
	 * @see org.eclipse.ui.internal.texteditor.AnnotationExpandHover#getHoverInfoForLine(org.eclipse.jface.text.source.ISourceViewer, int)
	 */
	protected Object getHoverInfoForLine(final ISourceViewer viewer, final int line) {
		IAnnotationModel model= viewer.getAnnotationModel();
		IDocument document= viewer.getDocument();
		
		if (model == null)
			return null;
		
		List exact= new ArrayList();
		HashMap messagesAtPosition= new HashMap();
		
		StyledText text= viewer.getTextWidget();
		Display display;
		if (text != null && !text.isDisposed())
			display= text.getDisplay();
		else
			display= null;
			
		
		Iterator e= model.getAnnotationIterator();
		while (e.hasNext()) {
			Annotation annotation= (Annotation) e.next();
			
			// don't prune deleted ones as we don't get many errors this way
//			if (annotation.isMarkedDeleted())
//				continue;
			
			if (fAnnotationAccess instanceof IAnnotationAccessExtension)
				if (!((IAnnotationAccessExtension)fAnnotationAccess).isPaintable(annotation))
					continue;
				
			if (annotation instanceof IJavaAnnotation && annotation instanceof IAnnotationPresentation)
				if (((IJavaAnnotation) annotation).getImage(display) == null)
					continue;
				
			AnnotationPreference pref= fLookup.getAnnotationPreference(annotation);
			if (pref != null) {
				String key= pref.getVerticalRulerPreferenceKey();
				if (key != null && !JavaPlugin.getDefault().getPreferenceStore().getBoolean(key))
					continue;
			}
			
			Position position= model.getPosition(annotation);
			if (position == null)
				continue;
			
			if (compareRulerLine(position, document, line) == 1) {
				
				if (isDuplicateMessage(messagesAtPosition, position, annotation.getText()))
					continue;
				
				exact.add(annotation);
			}
		}
		
		sort(exact, model);
		
		if (exact.size() > 0)
			setLastRulerMouseLocation(viewer, line);
		
		if (exact.size() > 0) {
			Annotation first= (Annotation) exact.get(0);
			if (!isBreakpointAnnotation(first))
				exact.add(0, new NoBreakpointAnnotation());
		}
		
		if (exact.size() <= 1)
//		if (exact.size() < 1)
			return null;
		
		AnnotationHoverInput input= new AnnotationHoverInput();
		input.fAnnotations= (Annotation[]) exact.toArray(new Annotation[0]);
		input.fViewer= viewer;
		input.fRulerInfo= fCompositeRuler;
		input.fAnnotationListener= fgListener;
		input.fDoubleClickListener= fDblClickListener;
		input.redoAction= new AnnotationExpansionControl.ICallback() {

			public void run(IInformationControlExtension2 control) {
				control.setInput(getHoverInfoForLine(viewer, line));
			}
			
		};
		input.model= model;
		
		return input;
	}
	
	/*
	 * @see org.eclipse.ui.internal.texteditor.AnnotationExpandHover#getOrder(org.eclipse.jface.text.source.Annotation)
	 */
	protected int getOrder(Annotation annotation) {
		if (isBreakpointAnnotation(annotation)) //$NON-NLS-1$
			return 1000;
		else
			return super.getOrder(annotation);
	}

	private boolean isBreakpointAnnotation(Annotation a) {
		if (a instanceof JavaMarkerAnnotation) {
			JavaMarkerAnnotation jma= (JavaMarkerAnnotation) a;
			// HACK to get breakpoints to show up first
			return jma.getType().equals("org.eclipse.debug.core.breakpoint"); //$NON-NLS-1$
		}
		return false;
	}
}
