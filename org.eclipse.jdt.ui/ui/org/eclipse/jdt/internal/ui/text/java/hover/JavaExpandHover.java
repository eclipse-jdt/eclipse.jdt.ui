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

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControlExtension2;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationAccess;
import org.eclipse.jface.text.source.IAnnotationAccessExtension;
import org.eclipse.jface.text.source.IAnnotationListener;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationPresentation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRulerInfo;

import org.eclipse.ui.internal.texteditor.AnnotationExpandHover;
import org.eclipse.ui.internal.texteditor.AnnotationExpansionControl;
import org.eclipse.ui.internal.texteditor.AnnotationExpansionControl.AnnotationHoverInput;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.javaeditor.IJavaAnnotation;
import org.eclipse.jdt.internal.ui.javaeditor.JavaMarkerAnnotation;

/**
 * 
 * 
 * @since 3.0
 */
public class JavaExpandHover extends AnnotationExpandHover {
	public static final String NO_BREAKPOINT_ANNOTATION= "org.eclipse.jdt.internal.ui.NoBreakpointAnnotation"; //$NON-NLS-1$

	private static class NoBreakpointAnnotation extends Annotation implements IAnnotationPresentation {
		
		NoBreakpointAnnotation() {
			super(NO_BREAKPOINT_ANNOTATION, false, "Double click to add a breakpoint"); //$NON-NLS-1$
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
	
	/**
	 * @param ruler
	 * @param listener
	 */
	public JavaExpandHover(IVerticalRulerInfo ruler, IAnnotationListener listener, IDoubleClickListener doubleClickListener, IAnnotationAccess access) {
		super(ruler, listener, doubleClickListener, access);
	}

	/**
	 * @param ruler
	 * @param access
	 */
	public JavaExpandHover(IVerticalRulerInfo ruler, IAnnotationAccess access) {
		super(ruler, access);
	}
	
	/*
	 * @see org.eclipse.ui.internal.texteditor.AnnotationExpandHover#getHoverInfo2(org.eclipse.jface.text.source.ISourceViewer, int)
	 */
	public Object getHoverInfo2(final ISourceViewer viewer, final int line) {
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
		input.fRulerInfo= fVerticalRulerInfo;
		input.fAnnotationListener= fAnnotationListener;
		input.fDoubleClickListener= fDblClickListener;
		input.redoAction= new AnnotationExpansionControl.ICallback() {

			public void run(IInformationControlExtension2 control) {
				control.setInput(getHoverInfo2(viewer, line));
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
	
	
	private static class FilteredAnnotationModel {
		private IAnnotationModel fModel;
		private List fFilteredAnnotations;
		private int fFirst;

		/**
		 * @param model
		 */
		public FilteredAnnotationModel(IAnnotationModel model) {
			fModel= model;
		}
		
		public Iterator getAnnotationIterator() {
			if (fFilteredAnnotations == null) {
				fFilteredAnnotations= new ArrayList();
				for (Iterator it= fModel.getAnnotationIterator(); it.hasNext();) {
					Annotation a= (Annotation) it.next();
					if (filter(a))
						fFilteredAnnotations.add(a);
				}
			}
			
			return fFilteredAnnotations.iterator();
		}
		
		/**
		 * @param a
		 * @return
		 */
		protected boolean filter(Annotation a) {
			// TODO Auto-generated method stub
			return false;
		}

		/**
		 * @param first
		 * @param count
		 */
		public void setValidLines(int first, int count) {
			fFirst= first;
			
		}
	}
	
	interface AnnotationFilter {
		/**
		 * Returns <code>true</code> if <code>annotation</code> passes this 
		 * filter, <code>false</code> if it does not.
		 * 
		 * @param annotation the <code>Annotation</code> to filter
		 * @return <code>true</code> if <code>annotation</code> passes the filter
		 */
		boolean filter(Annotation annotation);
	}
	
	static class LineAnnotationFilter implements AnnotationFilter {
		private int fCount;
		private int fFirst;

		/**
		 * @param first
		 * @param count
		 */
		public LineAnnotationFilter(int first, int count) {
			Assert.isTrue(first >= 0 && count >= 0);
			fFirst= first;
			fCount= count;
		}
		
		/*
		 * @see org.eclipse.jdt.internal.ui.text.java.hover.JavaExpandHover.AnnotationFilter#filter(org.eclipse.jface.text.source.Annotation)
		 */
		public boolean filter(Annotation annotation) {
			return false;
		}
	}

}
