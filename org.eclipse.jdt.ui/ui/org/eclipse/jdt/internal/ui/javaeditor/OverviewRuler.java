/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp. and others.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html

Contributors:
    IBM Corporation - Initial implementation
**********************************************************************/

package org.eclipse.jdt.internal.ui.javaeditor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension3;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelListener;
import org.eclipse.jface.text.source.IVerticalRulerInfo;

import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.ui.JavaPlugin;



/**
 * 
 */
public class OverviewRuler implements IVerticalRulerInfo {
	
	/**
	 * Internal listener class.
	 */
	class InternalListener implements ITextListener, IAnnotationModelListener {
		
		/*
		 * @see ITextListener#textChanged
		 */
		public void textChanged(TextEvent e) {		
			if (fTextViewer != null && e.getDocumentEvent() == null && e.getViewerRedrawState()) {
				// handle only changes of visible document
				redraw();
			}
		}
		
		/*
		 * @see IAnnotationModelListener#modelChanged(IAnnotationModel)
		 */
		public void modelChanged(IAnnotationModel model) {
			update();
		}
	}
	
	/**
	 * Filters problems based on their types.
	 */
	class FilterIterator implements Iterator {
		
		private final static int IGNORE= 0;
		private final static int TEMPORARY= 1;
		private final static int PERSISTENT= 2;
		
		private Iterator fIterator;
		private AnnotationType fType;
		private IProblemAnnotation fNext;
		private int fTemporary;
		
		public FilterIterator(AnnotationType type) {
			this(type, IGNORE);
		}
		
		public FilterIterator(AnnotationType type, boolean temporary) {
			this(type, temporary ? TEMPORARY : PERSISTENT);
		}
		
		private FilterIterator(AnnotationType type, int temporary) {
			fType= type;
			fTemporary= temporary;
			if (fModel != null) {
				fIterator= fModel.getAnnotationIterator();
				skip();
			}
		}
		
		private void skip() {
			while (fIterator.hasNext()) {
				Object next= fIterator.next();
				if (next instanceof IProblemAnnotation) {
					fNext= (IProblemAnnotation) next;
					AnnotationType type= fNext.getAnnotationType();
					if (fType == AnnotationType.ALL || fType == type) {
						if (fTemporary == IGNORE) return;
						if (fTemporary == TEMPORARY && fNext.isTemporary()) return;
						if (fTemporary == PERSISTENT && !fNext.isTemporary()) return;
					}
				}
			}
			fNext= null;
		}
		
		/*
		 * @see Iterator#hasNext()
		 */
		public boolean hasNext() {
			return fNext != null;
		}
		/*
		 * @see Iterator#next()
		 */
		public Object next() {
			try {
				return fNext;
			} finally {
				if (fModel != null)
					skip();
			}
		}
		/*
		 * @see Iterator#remove()
		 */
		public void remove() {
			throw new UnsupportedOperationException();
		}
	};
	
	private static final int INSET= 2;
	private static final int PROBLEM_HEIGHT_MIN= 4;
	private static boolean PROBLEM_HEIGHT_SCALABLE= false;
	
	
	/** The model of the overview ruler */
	private IAnnotationModel fModel;
	/** The view to which this ruler is connected */
	private ITextViewer fTextViewer;
	/** The ruler's canvas */
	private Canvas fCanvas;
	/** The drawable for double buffering */
	private Image fBuffer;
	/** The internal listener */
	private InternalListener fInternalListener= new InternalListener();
	/** The width of this vertical ruler */
	private int fWidth;
	/** The hit detection cursor */
	private Cursor fHitDetectionCursor;
	/** The last cursor */
	private Cursor fLastCursor;
	/** Cache for the actual scroll position in pixels */
	private int fScrollPos;
	/** The line of the last mouse button activity */
	private int fLastMouseButtonActivityLine= -1;
	/** The actual problem height */
	private int fProblemHeight= -1;
	
	private Set fAnnotationSet= new HashSet();
	private Map fLayers= new HashMap();
	private Map fColorTable= new HashMap();
	
	/**
	 * Constructs a vertical ruler with the given width.
	 *
	 * @param width the width of the vertical ruler
	 */
	public OverviewRuler(int width) {
		fWidth= width;		
	}
	
	public Control getControl() {
		return fCanvas;
	}
	
	public int getWidth() {
		return fWidth;
	}
	
	public void setModel(IAnnotationModel model) {
		if (model != fModel || model != null) {
			
			if (fModel != null)
				fModel.removeAnnotationModelListener(fInternalListener);
			
			fModel= model;
			
			if (fModel != null)
				fModel.addAnnotationModelListener(fInternalListener);
			
			update();
		}
	}	
	
	public Control createControl(Composite parent, ITextViewer textViewer) {
		
		fTextViewer= textViewer;
		
		fHitDetectionCursor= new Cursor(parent.getDisplay(), SWT.CURSOR_HAND);
		fCanvas= new Canvas(parent, SWT.NO_BACKGROUND);
		
		fCanvas.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent event) {
				if (fTextViewer != null)
					doubleBufferPaint(event.gc);
			}
		});
		
		fCanvas.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent event) {
				handleDispose();
				fTextViewer= null;		
			}
		});
		
		fCanvas.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent event) {
				handleMouseDown(event);
			}
		});
		
		fCanvas.addMouseMoveListener(new MouseMoveListener() {
			public void mouseMove(MouseEvent event) {
				handleMouseMove(event);
			}
		});
		
		if (fTextViewer != null)
			fTextViewer.addTextListener(fInternalListener);
		
		return fCanvas;
	}
	
	/**
	 * Disposes the ruler's resources.
	 */
	private void handleDispose() {
		
		if (fTextViewer != null) {
			fTextViewer.removeTextListener(fInternalListener);
			fTextViewer= null;
		}

		if (fModel != null)
			fModel.removeAnnotationModelListener(fInternalListener);

		if (fBuffer != null) {
			fBuffer.dispose();
			fBuffer= null;
		}
		
		if (fHitDetectionCursor != null) {
			fHitDetectionCursor.dispose();
			fHitDetectionCursor= null;
		}
		
		fAnnotationSet.clear();
		fLayers.clear();
		fColorTable.clear();
	}

	/**
	 * Double buffer drawing.
	 */
	private void doubleBufferPaint(GC dest) {
		
		Point size= fCanvas.getSize();
		
		if (size.x <= 0 || size.y <= 0)
			return;
		
		if (fBuffer != null) {
			Rectangle r= fBuffer.getBounds();
			if (r.width != size.x || r.height != size.y) {
				fBuffer.dispose();
				fBuffer= null;
			}
		}
		if (fBuffer == null)
			fBuffer= new Image(fCanvas.getDisplay(), size.x, size.y);
			
		GC gc= new GC(fBuffer);
		try {
			gc.setBackground(fCanvas.getBackground());
			gc.fillRectangle(0, 0, size.x, size.y);
			
			if (fTextViewer instanceof ITextViewerExtension3)
				doPaint1(gc);
			else
				doPaint(gc);
				
		} finally {
			gc.dispose();
		}
		
		dest.drawImage(fBuffer, 0, 0);
	}
	
	private void doPaint(GC gc) {
		
		if (fTextViewer == null)
			return;
			
		Rectangle r= new Rectangle(0, 0, 0, 0);
		int yy, hh= PROBLEM_HEIGHT_MIN;
		
		
		IDocument document= fTextViewer.getDocument();
		IRegion visible= fTextViewer.getVisibleRegion();
		
		StyledText textWidget= fTextViewer.getTextWidget();
		int maxLines= textWidget.getLineCount();
		fScrollPos= textWidget.getTopPixel();		
				
		Point size= fCanvas.getSize();
		int writable= maxLines * textWidget.getLineHeight();
		if (size.y > writable)
			size.y= writable;
		
		
		List indices= new ArrayList(fLayers.keySet());
		Collections.sort(indices);
		
		for (Iterator iterator= indices.iterator(); iterator.hasNext();) {
			Object layer= iterator.next();
			AnnotationType annotationType= (AnnotationType) fLayers.get(layer);
			
			if (skip(annotationType))
				continue;
			
			boolean[] temporary= new boolean[] { false, true };
			for (int t=0; t < temporary.length; t++) {
			
				Iterator e= new FilterIterator(annotationType, temporary[t]);
				Color fill= getFillColor(annotationType, temporary[t]);
				Color stroke= getStrokeColor(annotationType, temporary[t]);
				
				for (int i= 0; e.hasNext(); i++) {
					
					Annotation a= (Annotation) e.next();
					Position p= fModel.getPosition(a);
					
					if (p == null || !p.overlapsWith(visible.getOffset(), visible.getLength()))
						continue;
						
					int problemOffset= Math.max(p.getOffset(), visible.getOffset());
					int problemEnd= Math.min(p.getOffset() + p.getLength(), visible.getOffset() + visible.getLength());
					int problemLength= problemEnd - problemOffset;				
					
					try {
						if (PROBLEM_HEIGHT_SCALABLE) {
							int numbersOfLines= document.getNumberOfLines(problemOffset, problemLength);
							hh= (numbersOfLines * size.y) / maxLines;
							if (hh < PROBLEM_HEIGHT_MIN)
								hh= PROBLEM_HEIGHT_MIN;
						}
						fProblemHeight= hh;

						int startLine= textWidget.getLineAtOffset(problemOffset - visible.getOffset());
						yy= Math.min((startLine * size.y) / maxLines, size.y - hh);
							
						if (fill != null) {
							gc.setBackground(fill);
							gc.fillRectangle(INSET, yy, size.x-(2*INSET), hh);
						}
						
						if (stroke != null) {
							gc.setForeground(stroke);
							r.x= INSET;
							r.y= yy;
							r.width= size.x - (2 * INSET) - 1;
							r.height= hh;
							gc.setLineWidth(1);
							gc.drawRectangle(r);
						}
					} catch (BadLocationException x) {
					}
				}
			}
		}
	}
	
	private void doPaint1(GC gc) {

		if (fTextViewer == null)
			return;

		Rectangle r= new Rectangle(0, 0, 0, 0);
		int yy, hh= PROBLEM_HEIGHT_MIN;

		ITextViewerExtension3 extension= (ITextViewerExtension3) fTextViewer;
		IDocument document= fTextViewer.getDocument();		
		StyledText textWidget= fTextViewer.getTextWidget();
		fScrollPos= textWidget.getTopPixel();
		
		int maxLines= textWidget.getLineCount();
		Point size= fCanvas.getSize();
		int writable= maxLines * textWidget.getLineHeight();
		if (size.y > writable)
			size.y= writable;
			
		List indices= new ArrayList(fLayers.keySet());
		Collections.sort(indices);

		for (Iterator iterator= indices.iterator(); iterator.hasNext();) {
			Object layer= iterator.next();
			AnnotationType annotationType= (AnnotationType) fLayers.get(layer);

			if (skip(annotationType))
				continue;

			boolean[] temporary= new boolean[] { false, true };
			for (int t=0; t < temporary.length; t++) {

				Iterator e= new FilterIterator(annotationType, temporary[t]);
				Color fill= getFillColor(annotationType, temporary[t]);
				Color stroke= getStrokeColor(annotationType, temporary[t]);

				for (int i= 0; e.hasNext(); i++) {

					Annotation a= (Annotation) e.next();
					Position p= fModel.getPosition(a);

					if (p == null)
						continue;
						
					IRegion widgetRegion= extension.modelRange2WidgetRange(new Region(p.getOffset(), p.getLength()));
					if (widgetRegion == null)
						continue;
						
					try {
						if (PROBLEM_HEIGHT_SCALABLE) {
							int numbersOfLines= document.getNumberOfLines(p.getOffset(), p.getLength());
							hh= (numbersOfLines * size.y) / maxLines;
							if (hh < PROBLEM_HEIGHT_MIN)
								hh= PROBLEM_HEIGHT_MIN;
						}
						fProblemHeight= hh;

						int startLine= textWidget.getLineAtOffset(widgetRegion.getOffset());						
						yy= Math.min((startLine * size.y) / maxLines, size.y - hh);

						if (fill != null) {
							gc.setBackground(fill);
							gc.fillRectangle(INSET, yy, size.x-(2*INSET), hh);
						}

						if (stroke != null) {
							gc.setForeground(stroke);
							r.x= INSET;
							r.y= yy;
							r.width= size.x - (2 * INSET) - 1;
							r.height= hh;
							gc.setLineWidth(1);
							gc.drawRectangle(r);
						}
					} catch (BadLocationException x) {
					}
				}
			}
		}
	}

	/**
	 * Thread-safe implementation.
	 * Can be called from any thread.
	 */
	public void update() {
		if (fCanvas != null && !fCanvas.isDisposed()) {
			Display d= fCanvas.getDisplay();
			if (d != null) {
				d.asyncExec(new Runnable() {
					public void run() {
						redraw();
					}
				});
			}	
		}
	}
	
	/**
	 * Redraws the overview ruler.
	 */
	private void redraw() {
		if (fCanvas != null && !fCanvas.isDisposed()) {
			GC gc= new GC(fCanvas);
			doubleBufferPaint(gc);
			gc.dispose();
		}
	}
	
	private int[] toLineNumbers(int y_coordinate) {
					
		StyledText textWidget=  fTextViewer.getTextWidget();
		int maxLines= textWidget.getContent().getLineCount();
		
		int rulerLength= fCanvas.getSize().y;
		int writable= maxLines * textWidget.getLineHeight();

		if (rulerLength > writable)
			rulerLength= writable;

		if (y_coordinate >= writable)
			return new int[] {-1, -1};

		int[] lines= new int[2];
		
		int pixel= Math.max(y_coordinate - 1, 0);
		lines[0]= (pixel * maxLines) / rulerLength;
		
		pixel= Math.min(rulerLength, y_coordinate + 1);
		lines[1]= (pixel * maxLines) / rulerLength;
		
		if (fTextViewer instanceof ITextViewerExtension3) {
			ITextViewerExtension3 extension= (ITextViewerExtension3) fTextViewer;
			lines[0]= extension.widgetlLine2ModelLine(lines[0]);
			lines[1]= extension.widgetlLine2ModelLine(lines[1]);
		} else {
			try {
				IRegion visible= fTextViewer.getVisibleRegion();
				int lineNumber= fTextViewer.getDocument().getLineOfOffset(visible.getOffset());
				lines[0] += lineNumber;
				lines[1] += lineNumber;
			} catch (BadLocationException x) {
			}
		}
		
		return lines;
	}
	
	boolean hasAnnotationAt(int y_coordinate) {
		return findBestMatchingLineNumber(toLineNumbers(y_coordinate)) != -1;
	}
	
	private Position getProblemPositionAt(int[] lineNumbers) {
		if (lineNumbers[0] == -1)
			return null;
		
		Position found= null;
		
		try {
			IDocument d= fTextViewer.getDocument();
			IRegion line= d.getLineInformation(lineNumbers[0]);

			int start= line.getOffset();
			
			line= d.getLineInformation(lineNumbers[lineNumbers.length - 1]);
			int end= line.getOffset() + line.getLength();
			
			Iterator e= new FilterIterator(AnnotationType.ALL);
			while (e.hasNext()) {
				Annotation a= (Annotation) e.next();
				Position p= fModel.getPosition(a);
				if (start <= p.getOffset() && p.getOffset() < end) {
					if (found == null || p.getOffset() < found.getOffset())
						found= p;
				}
			}
			
		} catch (BadLocationException x) {
		}
		
		return found;
	}

	/**
	 * Returns the line which best corresponds to one of
	 * the underlying problem annotations at the given
	 * y ruler coordinate.
	 * 
	 * @return the best matching line or <code>-1</code> if no such line can be found
	 * @since 2.1
	 */
	private int findBestMatchingLineNumber(int[] lineNumbers) {
		if (lineNumbers == null || lineNumbers.length < 1)
			return -1;

		try {
			Position pos= getProblemPositionAt(lineNumbers);
			if (pos == null)
				return -1;
			return fTextViewer.getDocument().getLineOfOffset(pos.getOffset());
		} catch (BadLocationException ex) {
			return -1;
		}
	}

	private void handleMouseDown(MouseEvent event) {
		if (fTextViewer != null) {
			int[] lines= toLineNumbers(event.y);
			Position p= getProblemPositionAt(lines);
			if (p != null) {
				fTextViewer.revealRange(p.getOffset(), p.getLength());
				fTextViewer.setSelectedRange(p.getOffset(), p.getLength());
			}
			fTextViewer.getTextWidget().setFocus();
		}
		fLastMouseButtonActivityLine= toDocumentLineNumber(event.y);
	}
	
	private void handleMouseMove(MouseEvent event) {
		if (fTextViewer != null) {
			int[] lines= toLineNumbers(event.y);
			Position p= getProblemPositionAt(lines);
			Cursor cursor= (p != null ? fHitDetectionCursor : null);
			if (cursor != fLastCursor) {
				fCanvas.setCursor(cursor);
				fLastCursor= cursor;
			}
		}				
	}
	
	private void handleMouseDoubleClick(MouseEvent event) {
		fLastMouseButtonActivityLine= toDocumentLineNumber(event.y);
	}
	
	public void showAnnotation(AnnotationType annotationType, boolean show) {
		if (show)
			fAnnotationSet.add(annotationType);
		else
			fAnnotationSet.remove(annotationType);
	}
	
	public void setLayer(AnnotationType annotationType, int layer) {
		if (layer >= 0)
			fLayers.put(new Integer(layer), annotationType);
		else {
			Iterator e= fLayers.keySet().iterator();
			while (e.hasNext()) {
				Object key= e.next();
				if (annotationType.equals(fLayers.get(key))) {
					fLayers.remove(key);
					return;
				}
			}
		}
	}
	
	public void setColor(AnnotationType annotationType, Color color) {
		if (color != null)
			fColorTable.put(annotationType, color);
		else
			fColorTable.remove(annotationType);
	}
	
	private boolean skip(AnnotationType annotationType) {
		return !fAnnotationSet.contains(annotationType);
	}
	
	
	private static RGB interpolate(RGB fg, RGB bg, double scale) {
		return new RGB(
			(int) ((1.0-scale) * fg.red + scale * bg.red),
			(int) ((1.0-scale) * fg.green + scale * bg.green),
			(int) ((1.0-scale) * fg.blue + scale * bg.blue)
		);
	}
	
	private static double greyLevel(RGB rgb) {
		if (rgb.red == rgb.green && rgb.green == rgb.blue)
			return rgb.red;
		return  (0.299 * rgb.red + 0.587 * rgb.green + 0.114 * rgb.blue + 0.5);
	}
	
	private static boolean isDark(RGB rgb) {
		return greyLevel(rgb) > 128;
	}
	
	private static Color getColor(RGB rgb) {
		JavaTextTools textTools= JavaPlugin.getDefault().getJavaTextTools();
		return textTools.getColorManager().getColor(rgb);
	}
	
	private Color getColor(AnnotationType annotationType, double scale) {
		Color base= (Color) fColorTable.get(annotationType);
		if (base == null)
			return null;
			
		RGB baseRGB= base.getRGB();
		RGB background= fCanvas.getBackground().getRGB();
		
		boolean darkBase= isDark(baseRGB);
		boolean darkBackground= isDark(background);
		if (darkBase && darkBackground)
			background= new RGB(255, 255, 255);
		else if (!darkBase && !darkBackground)
			background= new RGB(0, 0, 0);
		
		return getColor(interpolate(baseRGB, background, scale));
	}
	
	private Color getStrokeColor(AnnotationType annotationType, boolean temporary) {
		return getColor(annotationType, temporary ? 0.5 : 0.2);
	}
	
	private Color getFillColor(AnnotationType annotationType, boolean temporary) {
		return getColor(annotationType, temporary ? 0.9 : 0.6);
	}
	
	/**
	 * @see IVerticalRulerInfo#getLineOfLastMouseButtonActivity()
	 * @since 2.1
	 */
	public int getLineOfLastMouseButtonActivity() {
		return fLastMouseButtonActivityLine;
	}

	/**
	 * @see IVerticalRulerInfo#toDocumentLineNumber(int)
	 * @since 2.1
	 */
	public int toDocumentLineNumber(int y_coordinate) {
		
		if (fTextViewer == null || y_coordinate == -1)
			return -1;

		int[] lineNumbers= toLineNumbers(y_coordinate);
		int bestLine= findBestMatchingLineNumber(lineNumbers);
		if (bestLine == -1 && lineNumbers.length > 0)
			return lineNumbers[0];
		return	bestLine;
	}

	/**
	 * Returns the height of the problem rectangle.
	 * 
	 * @return the height of the problem rectangle
	 * @since 2.1
	 */
	int getAnnotationHeight() {
		return fProblemHeight;
	}
}
