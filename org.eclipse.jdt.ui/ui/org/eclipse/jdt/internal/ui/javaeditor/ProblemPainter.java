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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelListener;
import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.internal.ui.JavaPlugin;


/**
 * Highlights the temporary problems.
 */
public class ProblemPainter implements IPainter, PaintListener, IAnnotationModelListener {	
	
	private static class ProblemPosition {
		Position fPosition;
		Color fColor;
		boolean fMultiLine;
	};
	
	private boolean fIsActive= false;
	private boolean fIsPainting= false;
	private boolean fIsSettingModel= false;
	
	private ITextEditor fTextEditor;
	private ISourceViewer fSourceViewer;
	private StyledText fTextWidget;
	private IAnnotationModel fModel;
	private List fProblemPositions= new ArrayList();
	
	private Map fColorTable= new HashMap();
	private Set fAnnotationSet= new HashSet();

	
	
	public ProblemPainter(ITextEditor textEditor, ISourceViewer sourceViewer) {
		fTextEditor= textEditor;
		fSourceViewer= sourceViewer;
		fTextWidget= sourceViewer.getTextWidget();
	}
	
	private boolean hasProblems() {
		return !fProblemPositions.isEmpty();
	}	
	
	private void enablePainting() {
		if (!fIsPainting && hasProblems()) {
			fIsPainting= true;
			fTextWidget.addPaintListener(this);
			handleDrawRequest(null);
		}
	}
	
	private void disablePainting(boolean redraw) {
		if (fIsPainting) {
			fIsPainting= false;
			fTextWidget.removePaintListener(this);
			if (redraw && hasProblems())
				handleDrawRequest(null);
		}
	}
	
	private void setModel(IAnnotationModel model) {
		if (fModel != model) {
			if (fModel != null)
				fModel.removeAnnotationModelListener(this);
			fModel= model;
			if (fModel != null) {
				try {
					fIsSettingModel= true;
					fModel.addAnnotationModelListener(this);
				} finally {
					fIsSettingModel= false;
				}
			}
		}
	}
	
	private void catchupWithModel() {	
		if (fProblemPositions != null) {
			fProblemPositions.clear();
			if (fModel != null) {
				
				Iterator e= new ProblemAnnotationIterator(fModel, true);
				while (e.hasNext()) {
					IProblemAnnotation pa= (IProblemAnnotation) e.next();
					Annotation a= (Annotation) pa;
					
					Color color= null;
					AnnotationType type= pa.getAnnotationType();
					if (fAnnotationSet.contains(type))
						color= (Color) fColorTable.get(type);
										
					if (color != null) {
						ProblemPosition pp= new ProblemPosition();
						pp.fPosition= fModel.getPosition(a);
						pp.fColor= color;
						pp.fMultiLine= true;
						fProblemPositions.add(pp);
					}
				}
			}
		}
	}
	
	private void updatePainting() {
		disablePainting(true);
		catchupWithModel();							
		enablePainting();
	}
	
	/*
	 * @see IAnnotationModelListener#modelChanged(IAnnotationModel)
	 */
	public void modelChanged(final IAnnotationModel model) {
		if (fTextWidget != null && !fTextWidget.isDisposed()) {
			if (fIsSettingModel) {
				// inside the ui thread -> no need for posting
				updatePainting();
			} else {
				Display d= fTextWidget.getDisplay();
				if (d != null) {
					d.asyncExec(new Runnable() {
						public void run() {
							if (fTextWidget != null && !fTextWidget.isDisposed())
								updatePainting();
						}
					});
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
	
	public void paintAnnotations(AnnotationType annotationType, boolean paint) {
		if (paint)
			fAnnotationSet.add(annotationType);
		else
			fAnnotationSet.remove(annotationType);
	}
	
	public boolean isPaintingAnnotations() {
		return !fAnnotationSet.isEmpty();
	}
	
	/*
	 * @see IPainter#dispose()
	 */
	public void dispose() {
		
		if (fColorTable != null)	
			fColorTable.clear();
		fColorTable= null;
		
		if (fAnnotationSet != null)
			fAnnotationSet.clear();
		fAnnotationSet= null;
		
		fTextWidget= null;
		fModel= null;
		fProblemPositions= null;
	}

	/*
	 * Returns the document offset of the upper left corner of the widgets viewport,
	 * possibly including partially visible lines.
	 */
	private int getInclusiveTopIndexStartOffset() {
		
		if (fTextWidget != null && !fTextWidget.isDisposed()) {	
			int top= fSourceViewer.getTopIndex();
			if ((fTextWidget.getTopPixel() % fTextWidget.getLineHeight()) != 0)
				top--;
			try {
				IDocument document= fSourceViewer.getDocument();
				return document.getLineOffset(top);
			} catch (BadLocationException ex) {
			}
		}
		
		return -1;
	}
	
	/*
	 * @see PaintListener#paintControl(PaintEvent)
	 */
	public void paintControl(PaintEvent event) {
		if (fTextWidget != null)
			handleDrawRequest(event.gc);
	}
	
	private void handleDrawRequest(GC gc) {

		IRegion region= fSourceViewer.getVisibleRegion();
		int offset= region.getOffset();
		int length= region.getLength();

		int vOffset= getInclusiveTopIndexStartOffset();
		// http://bugs.eclipse.org/bugs/show_bug.cgi?id=17147
		int vLength= fSourceViewer.getBottomIndexEndOffset() + 1;		
		
		for (Iterator e = fProblemPositions.iterator(); e.hasNext();) {
			ProblemPosition pp = (ProblemPosition) e.next();
			Position p= pp.fPosition;
			if (p.overlapsWith(vOffset, vLength) && p.overlapsWith(offset , length)) {
				int p1= Math.max(offset, p.getOffset());
				int p2= Math.min(offset + length, p.getOffset() + p.getLength());
				
				if (!pp.fMultiLine) {
					
					draw(gc, p1 - offset, p2 - p1, pp.fColor);
				
				} else {
					
					IDocument document= fSourceViewer.getDocument();
					try {
						
						int startLine= document.getLineOfOffset(p1); 
						int lastInclusive= Math.max(p1, p2 - 1);
						int endLine= document.getLineOfOffset(lastInclusive);
						
						for (int i= startLine; i <= endLine; i++) {
							IRegion line= document.getLineInformation(i);
							int paintStart= Math.max(line.getOffset(), p1);
							int paintEnd= Math.min(line.getOffset() + line.getLength(), p2);
							draw(gc, paintStart - offset, paintEnd - paintStart, pp.fColor);
						}
					
					} catch (BadLocationException x) {
					}
				}
			}
		}
	}
	
	private int[] computePolyline(Point left, Point right, int height) {
		
		final int WIDTH= 4; // must be even
		final int HEIGHT= 2; // can be any number
//		final int MINPEEKS= 2; // minimal number of peeks
		
		int peeks= (right.x - left.x) / WIDTH;
//		if (peeks < MINPEEKS) {
//			int missing= (MINPEEKS - peeks) * WIDTH;
//			left.x= Math.max(0, left.x - missing/2);
//			peeks= MINPEEKS;
//		}
		
		int leftX= left.x;
				
		// compute (number of point) * 2
		int length= ((2 * peeks) + 1) * 2;
		if (length < 0)
			return new int[0];
			
		int[] coordinates= new int[length];
		
		// cache peeks' y-coordinates
		int bottom= left.y + height - 1;
		int top= bottom - HEIGHT;
		
		// populate array with peek coordinates
		for (int i= 0; i < peeks; i++) {
			int index= 4 * i;
			coordinates[index]= leftX + (WIDTH * i);
			coordinates[index+1]= bottom;
			coordinates[index+2]= coordinates[index] + WIDTH/2;
			coordinates[index+3]= top;
		}
		
		// the last down flank is missing
		coordinates[length-2]= left.x + (WIDTH * peeks);
		coordinates[length-1]= bottom;
		
		return coordinates;
	}
	
	private void draw(GC gc, int offset, int length, Color color) {
		if (gc != null) {
			
			Point left= fTextWidget.getLocationAtOffset(offset);
			Point right= fTextWidget.getLocationAtOffset(offset + length);
			
			gc.setForeground(color);
			int[] polyline= computePolyline(left, right, gc.getFontMetrics().getHeight());
			gc.drawPolyline(polyline);
								
		} else {
			fTextWidget.redrawRange(offset, length, true);
		}
	}
	
	/*
	 * @see IPainter#deactivate(boolean)
	 */
	public void deactivate(boolean redraw) {
		if (fIsActive) {
			fIsActive= false;
			disablePainting(redraw);
			setModel(null);
			catchupWithModel();
		}
	}
	
	/*
	 * @see IPainter#paint(int)
	 */
	public void paint(int reason) {
		if (!fIsActive) {
			fIsActive= true;
			IDocumentProvider provider= JavaPlugin.getDefault().getCompilationUnitDocumentProvider();
			setModel(provider.getAnnotationModel(fTextEditor.getEditorInput()));
		} else if (CONFIGURATION == reason || INTERNAL == reason)
			updatePainting();
	}

	/*
	 * @see IPainter#setPositionManager(IPositionManager)
	 */
	public void setPositionManager(IPositionManager manager) {
	}
}
