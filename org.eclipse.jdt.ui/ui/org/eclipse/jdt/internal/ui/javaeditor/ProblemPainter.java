package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.jdt.core.compiler.IProblem;


/**
 * Highlights the temporary problems.
 */
public class ProblemPainter implements IPainter, PaintListener {
	
	
	private boolean fIsActive= false;
	private StyledText fTextWidget;
	private ISourceViewer fSourceViewer;
	private Color fColor;
	private IPositionManager fPositionManager;
	
	private List fProblemPositions= new ArrayList(5);
		
	
	public ProblemPainter(ISourceViewer sourceViewer) {
		fTextWidget= sourceViewer.getTextWidget();
		fSourceViewer= sourceViewer;
	}
	
	public List getProblemPositions() {
		return fProblemPositions;
	}
	
	public void setHighlightColor(Color color) {
		fColor= color;
	}
	
	public void dispose() {
		fColor= null;
		fTextWidget= null;
	}
	
	private void setProblems(List problems) {
		for (Iterator e = problems.iterator(); e.hasNext();) {
			ProblemPosition p= new ProblemPosition((IProblem) e.next());
			fProblemPositions.add(p);
			if (fPositionManager != null)
				fPositionManager.addManagedPosition(p);
		}
	}
	
	public void updateProblems(List problems) {
		deactivate(true);
		setProblems(problems);
		paint();
	}
	
	private void removePositions() {
		if (fPositionManager != null) {
			for (Iterator e = fProblemPositions.iterator(); e.hasNext();) {
				ProblemPosition p = (ProblemPosition) e.next();
				fPositionManager.removeManagedPosition(p);
			}
		}
	}
	
	private void removeProblems() {
		removePositions();
		fProblemPositions.clear();
	}
	
	private boolean hasProblems() {
		return !fProblemPositions.isEmpty();
	}
	
	public void deactivate(boolean redraw) {
		if (fIsActive) {
			fIsActive= false;
			fTextWidget.removePaintListener(this);
			if (hasProblems()) {
				if (redraw)
					handleDrawRequest(null);
				removeProblems();
			}
		}
	}
	
	public void paintControl(PaintEvent event) {
		if (fTextWidget != null && hasProblems())
			handleDrawRequest(event.gc);
	}
	
	private void handleDrawRequest(GC gc) {
		
		IRegion region= fSourceViewer.getVisibleRegion();
		int offset= region.getOffset();
		int length= region.getLength();
		
		for (Iterator e = fProblemPositions.iterator(); e.hasNext();) {
			ProblemPosition p = (ProblemPosition) e.next();
			if (p.overlapsWith(offset, length))
				draw(gc, p.getOffset() - offset, p.getLength());
		}
	}
	
	private int[] computePolyline(Point left, Point right, int height) {
		
		final int WIDTH= 4; // must be even
		final int HEIGHT= 2; // can be any number
		
		int leftX= left.x;
		int peeks= (right.x - left.x) / WIDTH;
				
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
	
	private void draw(GC gc, int offset, int length) {
		if (gc != null) {
			
			Point left= fTextWidget.getLocationAtOffset(offset);
			Point right= fTextWidget.getLocationAtOffset(offset + length);
			
			gc.setForeground(fColor);
			int[] polyline= computePolyline(left, right, gc.getFontMetrics().getHeight());
			gc.drawPolyline(polyline);
								
		} else {
			fTextWidget.redrawRange(offset, length, true);
		}
	}
	
	/*
	 * @see IPainter#paint()
	 */
	public void paint() {
		if (fIsActive) {
			
			if (hasProblems())
				handleDrawRequest(null);
			else
				deactivate(false);
				
		} else if (hasProblems()) {
			fIsActive= true;
			fTextWidget.addPaintListener(this);
			handleDrawRequest(null);
		}
	}

	/*
	 * @see IPainter#setPositionManager(IPositionManager)
	 */
	public void setPositionManager(IPositionManager manager) {
		if (manager != fPositionManager && fPositionManager != null)
			removePositions();
		fPositionManager= manager;
	}
}
