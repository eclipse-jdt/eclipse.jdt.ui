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

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension3;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;

/**
 * Responsible for painting the linked mode indication
 * 
 * @since 3.0
 */
class LinkedUIPainter implements PaintListener {
	private ITextViewer fViewer;
	private Position fFramePosition;
	private Color fFrameColor;

	LinkedUIPainter(ITextViewer viewer) {
		Assert.isNotNull(viewer);
		
		fViewer= viewer;
	}
	
	void setPosition(Position position) {
		if (position == null || !position.equals(fFramePosition)) {
			fFramePosition= position;
		}
	}
	
	void setColor(Color color) {
		if (color == null || !color.equals(fFrameColor)) {
			if (fFrameColor != null)
				fFrameColor.dispose();
			fFrameColor= color;
		}
	}
	
	void dispose() {
		setColor(null);
		setPosition(null);
		fViewer= null;
	}

	void redraw() {
		IRegion widgetRange= asWidgetRange(fFramePosition);
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
		if (fFramePosition == null || fFrameColor == null)
			return;
			
		IRegion widgetRange= asWidgetRange(fFramePosition);
		if (widgetRange == null) {
			return;
		}

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
		gc.setForeground(fFrameColor);
		gc.drawLine(x1, y, x2, y);
	}

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

	private IRegion asWidgetRange(Position position) {
		if (fViewer instanceof ITextViewerExtension3) {
			
			ITextViewerExtension3 extension= (ITextViewerExtension3) fViewer;
			return extension.modelRange2WidgetRange(new Region(position.getOffset(), position.getLength()));
		
		} else {
			
			IRegion region= fViewer.getVisibleRegion();
			if (includes(region, position))
				return new Region(position.getOffset() -  region.getOffset(), position.getLength());
		}
		
		return null;
	}

	private static boolean includes(IRegion region, Position position) {
		return
			position.getOffset() >= region.getOffset() &&
			position.getOffset() + position.getLength() <= region.getOffset() + region.getLength();
	}

	public void setViewer(ITextViewer viewer) {
		Assert.isNotNull(viewer);
		fViewer= viewer;
	}

}
