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
package org.eclipse.jdt.internal.ui.text.link;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension3;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;

/**
 * Responsible for painting the linked mode decorations.
 * 
 * @since 3.0
 */
class LinkedUIPainter implements PaintListener {
	
	/** No painting (value: {@value}). */
	private static final int NONE= 0;
	/** Paints an underlined position (value: {@value}). */
	private static final int UNDERLINE= 1;
	/** Paints a boxed position (value: {@value}). */
	private static final int BOX= 2;
	/** Paints a vertical line (value: {@value}). */
	private static final int VLINE= 3;

	// configure drawing of master, slave, and target position
	// TODO come up with a good setting / allow configuration
	private static final int MASTER= BOX;
	private static final int SLAVES= NONE;
	private static final int TARGETS= BOX;
	private static final int EXIT= VLINE;
	
	
	/**
	 * Draws a position on the <code>GC</code> inside a <code>PaintEvent</code>.
	 */
	private interface DrawingStrategy {
		/**
		 * Draws decorations for a <code>Position</code> on the <code>GC</code>
		 * in <code>event</code> using <code>color</code>.
		 * 
		 * @param event the event to get the <code>GC</code> from
		 * @param position the position to decorate
		 * @param color the color to draw with
		 */
		void draw(PaintEvent event, Position position, Color color);
	}
	
	/**
	 * Decorates positions with a surrounding box.
	 */
	private class BoxPainter implements DrawingStrategy {
		/*
		 * @see org.eclipse.jdt.internal.ui.text.link.LinkedUIPainter.Painter#draw(org.eclipse.swt.events.PaintEvent, org.eclipse.jface.text.Position, org.eclipse.swt.graphics.Color)
		 */
		public void draw(PaintEvent event, Position position, Color color) {
			IRegion widgetRange= asWidgetRange(position);
			if (widgetRange == null)
				return;

			int offset= widgetRange.getOffset();
			int length= widgetRange.getLength();

			StyledText text= fViewer.getTextWidget();
			
			// support for bidi
			Point minLocation= getMinimumLocation(text, offset, length);
			Point maxLocation= getMaximumLocation(text, offset, length);

			int x1= minLocation.x;
			int x2= minLocation.x + maxLocation.x - minLocation.x - 1;
			int y1= minLocation.y;
			int y2= minLocation.y + text.getLineHeight() - 1;
			
			GC gc= event.gc;
			gc.setForeground(color);
			gc.drawRectangle(x1, y1, x2 - x1, y2 - y1);
		}
	}
	
	/**
	 * Decorates position with an underline.
	 */
	private class UnderlinePainter implements DrawingStrategy {
		/*
		 * @see org.eclipse.jdt.internal.ui.text.link.LinkedUIPainter.Painter#draw(org.eclipse.swt.events.PaintEvent, org.eclipse.jface.text.Position, org.eclipse.swt.graphics.Color)
		 */
		public void draw(PaintEvent event, Position position, Color color) {
			IRegion widgetRange= asWidgetRange(position);
			if (widgetRange == null)
				return;

			int offset= widgetRange.getOffset();
			int length= widgetRange.getLength();
			
			StyledText text= fViewer.getTextWidget();
			
			// support for bidi
			Point minLocation= getMinimumLocation(text, offset, length);
			Point maxLocation= getMaximumLocation(text, offset, length);
			
			int x1= minLocation.x;
			int x2= minLocation.x + maxLocation.x - minLocation.x - 1;
			int y= minLocation.y + text.getLineHeight() - 1;
			
			GC gc= event.gc;
			gc.setForeground(color);
			gc.drawLine(x1, y, x2, y);
		}
	}
	
	/**
	 * Decorates position with an underline.
	 */
	private class VerticalLinePainter implements DrawingStrategy {
		/*
		 * @see org.eclipse.jdt.internal.ui.text.link.LinkedUIPainter.Painter#draw(org.eclipse.swt.events.PaintEvent, org.eclipse.jface.text.Position, org.eclipse.swt.graphics.Color)
		 */
		public void draw(PaintEvent event, Position position, Color color) {
			IRegion widgetRange= asWidgetRange(position);
			if (widgetRange == null)
				return;

			int offset= widgetRange.getOffset();
			
			StyledText text= fViewer.getTextWidget();
			
			// support for bidi
			Point minLocation= getMinimumLocation(text, offset, 0);
			
			GC gc= event.gc;
			gc.setForeground(color);
			gc.drawLine(minLocation.x, minLocation.y + 1, minLocation.x, minLocation.y + text.getLineHeight() - 2);
		}
	}
	
	/**
	 * No decorations.
	 */
	private class NonePainter implements DrawingStrategy {
		/*
		 * @see org.eclipse.jdt.internal.ui.text.link.LinkedUIPainter.Painter#draw(org.eclipse.swt.events.PaintEvent, org.eclipse.jface.text.Position, org.eclipse.swt.graphics.Color)
		 */
		public void draw(PaintEvent event, Position pos, Color col) {
			// do nothing
		}
	}
	
	/** The viewer. */
	private ITextViewer fViewer;
	/** The master position. */
	private LinkedPosition fMasterPosition;
	/** The slave positions. */
	private List fSlavePositions;
	/** The target positions (unfiltered). */
	private List fTargetPositions;
	/** The current target positions (recomputed every time the master position changes). */
	private List fCurrentTargetPositions;
	/** The exit position. */
	private Position fExitPosition;
	/** The linked environment to find out about the slave positions of the master. */
	private final LinkedEnvironment fEnv;
	/** The master color. */
	private Color fMasterColor;
	/** The slave color. */
	private Color fSlaveColor;
	/** The target color. */
	private Color fTargetColor;
	/** The exit color. */
	private Color fExitColor;
	/** The master drawing strategy. */
	private DrawingStrategy fMasterPainter;
	/** The slave drawing strategy. */
	private DrawingStrategy fSlavePainter;
	/** The target drawing strategy. */
	private DrawingStrategy fTargetPainter;
	/** The exit drawing strategy. */
	private DrawingStrategy fExitPainter;

	/**
	 * Creates a new painter.
	 * 
	 * @param viewer the viewer to draw on initially
	 * @param env the <code>LinkedEnvironment</code> to get group information from
	 */
	LinkedUIPainter(ITextViewer viewer, LinkedEnvironment env) {
		Assert.isNotNull(viewer);
		Assert.isNotNull(env);
		
		fEnv= env;
		fViewer= viewer;
		
		fMasterPainter= getPainter(MASTER);
		fSlavePainter= getPainter(SLAVES);
		fTargetPainter= getPainter(TARGETS);
		fExitPainter= getPainter(EXIT);
	}
	
	/**
	 * Returns the drawing strategy corresponding to <code>style</code>.
	 * 
	 * @param style the drawing style
	 * @return the corresponding strategy
	 */
	private DrawingStrategy getPainter(int style) {
		switch (style) {
			case UNDERLINE:
				return new UnderlinePainter();
			case BOX:
				return new BoxPainter();
			case VLINE:
				return new VerticalLinePainter();
			case NONE:
			default:
				return new NonePainter();
		}
	}

	/**
	 * Sets the master position.
	 * 
	 * @param position the new master position
	 */
	public void setMasterPosition(LinkedPosition position) {
		if (fViewer == null) // we're disposed
			return;
		IDocument doc= fViewer.getDocument();
		if (doc == null || fMasterPosition == position)
			return;
		
		fMasterPosition= position;
		fCurrentTargetPositions= new ArrayList(fTargetPositions);
		fSlavePositions= new ArrayList();
		if (fMasterPosition != null) {
			LinkedPositionGroup linkedGroup= fEnv.getGroupForPosition(fMasterPosition);
			if (linkedGroup != null)
				fSlavePositions= new ArrayList(Arrays.asList(linkedGroup.getPositions()));
		} 
		

		if (fMasterPosition == null || !doc.equals(fMasterPosition.getDocument()))
			// position is not valid if not in this document
			fMasterPosition= null;
		
		fSlavePositions.removeAll(fCurrentTargetPositions);
		fCurrentTargetPositions.remove(fMasterPosition);
		fCurrentTargetPositions.remove(fExitPosition);
		fSlavePositions.remove(fMasterPosition);
		prune(fCurrentTargetPositions, doc);
		prune(fSlavePositions, doc);
	}

	/**
	 * Prune <code>list</code> of all <code>LinkedPosition</code>s that 
	 * do not belong to the receiver's viewer's <code>IDocument</code>.
	 * 
	 * @param list the list of positions to prune
	 */
	private void prune(List list, IDocument fDocument) {
		for (Iterator iter= list.iterator(); iter.hasNext();) {
			LinkedPosition pos= (LinkedPosition) iter.next();
			if (!pos.getDocument().equals(fDocument))
				iter.remove();
		}
	}

	/**
	 * Sets the target positions (the jump targets in a linked setup).
	 * 
	 * @param positions the new positions - note that the array is not copied
	 */
	public void setTargetPositions(Position[] positions) {
		fTargetPositions= Arrays.asList(positions);
	}
	
	/**
	 * Sets the exit position.
	 * 
	 * @param position the exit position
	 */
	public void setExitPosition(Position position) {
		fExitPosition= position;
	}
	
	/**
	 * Disposes of the painter - later calls to <code>paintControl</code> will
	 * be ignored.
	 */
	public void dispose() {
		setMasterColor(null);
		setSlaveColor(null);
		setTargetColor(null);
		setMasterPosition(null);
		setTargetPositions(new Position[0]);
		fViewer= null;
	}

	/**
	 * Invalidates any positions that would currently be decorated, causing a
	 * redraw of the respective regions.
	 */
	public void redraw() {
		if (fViewer == null)
			return;
		
		if (MASTER != NONE)
			redrawPosition(fMasterPosition);
		
		if (SLAVES != NONE && fSlavePositions != null)
			for (Iterator it= fSlavePositions.iterator(); it.hasNext();) {
				LinkedPosition p= (LinkedPosition) it.next();
				redrawPosition(p);
			}
		if (TARGETS != NONE && fCurrentTargetPositions != null)
			for (Iterator it= fCurrentTargetPositions.iterator(); it.hasNext();) {
				LinkedPosition p= (LinkedPosition) it.next();
				redrawPosition(p);
			}
		
		if (EXIT != NONE && fExitPosition != null)
			redrawPosition(fExitPosition);
		
	}
	
	/**
	 * Invalidates a single position, causing the respective region to be redrawn.
	 * 
	 * @param pos the <code>Position</code> to redraw
	 */
	private void redrawPosition(Position pos) {
		if (pos == null)
			return;
		
		IRegion widgetRange= asWidgetRange(pos);
		if (widgetRange == null) {
			return;
		}
		
		StyledText text= fViewer.getTextWidget();
		if (text != null && !text.isDisposed())	
			text.redrawRange(widgetRange.getOffset(), widgetRange.getLength(), true);
		
	}

	/*
	 * @see org.eclipse.swt.events.PaintListener#paintControl(org.eclipse.swt.events.PaintEvent)
	 */
	public void paintControl(PaintEvent event) {
		if (fMasterPosition == null || fMasterColor == null)
			return;

		fMasterPainter.draw(event, fMasterPosition, fMasterColor);
		if (fSlavePositions != null)
			for (Iterator it= fSlavePositions.iterator(); it.hasNext();) {
				Position p= (Position) it.next();
				fSlavePainter.draw(event, p, fSlaveColor);
			}
		
		if (fCurrentTargetPositions != null)
			for (Iterator it= fCurrentTargetPositions.iterator(); it.hasNext();) {
				Position p= (Position) it.next();
				fTargetPainter.draw(event, p, fTargetColor);
			}
		
		if (fExitPosition != null)
			fExitPainter.draw(event, fExitPosition, fExitColor);
	}

	/**
	 * Returns the minimal location of an offset/length pair in a <code>StyledText</code>.
	 * This method respects BIDI setups.
	 * 
	 * @param text the text widget to query
	 * @param offset the offset of the position to compute the minimal location for
	 * @param length the length of the position to compute the minimal location for
	 * @return the minimal location in <code>text</code> for the given offset / length
	 */
	private static Point getMinimumLocation(StyledText text, int offset, int length) {
		Point minLocation= new Point(Integer.MAX_VALUE, Integer.MAX_VALUE);

		for (int i= 0; i <= length; i++) {
			Point location= text.getLocationAtOffset(offset + i);
			
			if (location.x < minLocation.x)
				minLocation.x= location.x;			
			if (location.y < minLocation.y)
				minLocation.y= location.y;			
		}	
		
		return minLocation;
	}

	/**
	 * Returns the maximal location of an offset/length pair in a <code>StyledText</code>.
	 * This method respects BIDI setups.
	 * 
	 * @param text the text widget to query
	 * @param offset the offset of the position to compute the maximal location for
	 * @param length the length of the position to compute the maximal location for
	 * @return the maximal location in <code>text</code> for the given offset / length
	 */
	private static Point getMaximumLocation(StyledText text, int offset, int length) {
		Point maxLocation= new Point(Integer.MIN_VALUE, Integer.MIN_VALUE);

		for (int i= 0; i <= length; i++) {
			Point location= text.getLocationAtOffset(offset + i);
			
			if (location.x > maxLocation.x)
				maxLocation.x= location.x;			
			if (location.y > maxLocation.y)
				maxLocation.y= location.y;			
		}	
		
		return maxLocation;
	}

	/**
	 * Returns the region covered by <code>position</code> in widget coordinates.
	 * 
	 * @param position the <code>Position</code> to convert to widget coordinates
	 * @return the widget region covered by <code>position</code>
	 */
	private IRegion asWidgetRange(Position position) {
		int length= position.getLength();
		if (fViewer instanceof ITextViewerExtension3) {
			
			ITextViewerExtension3 extension= (ITextViewerExtension3) fViewer;
			return extension.modelRange2WidgetRange(new Region(position.getOffset(), length));
		
		} else {
			
			IRegion region= fViewer.getVisibleRegion();
			if (includes(region, position))
				return new Region(position.getOffset() -  region.getOffset(), length);
		}
		
		return null;
	}

	/**
	 * Returns whether <code>region</code> includes <code>position</code>.
	 * 
	 * @param region the region that is tested
	 * @param position the position to test
	 * @return <code>true</code> if <code>position</code> lies within <code>region</code>
	 */
	private static boolean includes(IRegion region, Position position) {
		return
			position.getOffset() >= region.getOffset() &&
			position.getOffset() + position.getLength() <= region.getOffset() + region.getLength();
	}

	/**
	 * Sets the viewer that the painter currently draws on.
	 * 
	 * @param viewer the new viewer
	 */
	public void setViewer(ITextViewer viewer) {
		Assert.isNotNull(viewer);
		fViewer= viewer;
	}

	/**
	 * Sets the master color.
	 * 
	 * @param color the new master color
	 */
	public void setMasterColor(Color color) {
		if (color == null || !color.equals(fMasterColor)) {
			if (fMasterColor != null)
				fMasterColor.dispose();
			fMasterColor= color;
		}
	}
	
	/**
	 * Sets the slave color.
	 * 
	 * @param color the new slave color
	 */
	public void setSlaveColor(Color color) {
		if (color == null || !color.equals(fSlaveColor)) {
			if (fSlaveColor != null)
				fSlaveColor.dispose();
			fSlaveColor= color;
		}
	}
	
	/**
	 * Sets the exit color.
	 * 
	 * @param color the new exit color
	 */
	public void setTargetColor(Color color) {
		if (color == null || !color.equals(fTargetColor)) {
			if (fTargetColor != null)
				fTargetColor.dispose();
			fTargetColor= color;
		}
	}
	
	/**
	 * Sets the target color.
	 * 
	 * @param color the new target color
	 */
	public void setExitColor(Color color) {
		if (color == null || !color.equals(fExitColor)) {
			if (fExitColor != null)
				fExitColor.dispose();
			fExitColor= color;
		}
	}
}
