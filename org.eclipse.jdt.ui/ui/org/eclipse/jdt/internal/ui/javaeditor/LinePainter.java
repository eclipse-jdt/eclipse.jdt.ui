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


import org.eclipse.swt.custom.LineBackgroundEvent;
import org.eclipse.swt.custom.LineBackgroundListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.ISourceViewer;


public class LinePainter implements IPainter, LineBackgroundListener {

	private final ISourceViewer fViewer;
	private Color fHighlightColor;
	private IPositionManager fPositionManager;

	// positions to keep track of beginning and end of line to be painted or cleared
	private Position fCurrentLine= new Position(0, 0);
	private Position fLastLine= new Position(0, 0);
	// used to keep track of the last line painted
	private int fLastLineNumber= -1;
	private boolean fIsActive;

	public LinePainter(ISourceViewer sourceViewer) {
		fViewer= sourceViewer;
	}

	public void setHighlightColor(Color highlightColor) {
		fHighlightColor= highlightColor;
	}

	/*
	 * @see LineBackgroundListener#lineGetBackground(LineBackgroundEvent)
	 */
	public void lineGetBackground(LineBackgroundEvent event) {
		// don't use cached line information because of asynch painting

		StyledText textWidget= fViewer.getTextWidget();
		if (textWidget != null) {
			
			int caret= textWidget.getCaretOffset();
			int length= event.lineText.length();

			if (event.lineOffset <= caret && caret <= event.lineOffset + length)
				event.lineBackground= fHighlightColor;
			else
				event.lineBackground= textWidget.getBackground();
		}
	}

	private boolean updateHighlightLine() {
		try {

			IDocument document= fViewer.getDocument();

			int offset= fViewer.getTextWidget().getCaretOffset() + fViewer.getVisibleRegion().getOffset();
			int lineNumber= document.getLineOfOffset(offset);
						
			// redraw if the current line number is different from the last line number we painted
			// initially fLastLineNumber is -1
			if (lineNumber != fLastLineNumber) {
				
				fLastLine.offset= fCurrentLine.offset;
				fLastLine.length= fCurrentLine.length;
				fLastLine.isDeleted= fCurrentLine.isDeleted;

				fCurrentLine.isDeleted= false;
				fCurrentLine.offset= document.getLineOffset(lineNumber);
				if (lineNumber == document.getNumberOfLines() - 1)
					fCurrentLine.length= document.getLength() - fCurrentLine.offset;
				else
					fCurrentLine.length=	document.getLineOffset(lineNumber + 1) - fCurrentLine.offset;
				
				fLastLineNumber= lineNumber;
				return true;
				
			}
			
		} catch (BadLocationException e) {
		}

		return false;
	}

	private void drawHighlightLine(Position position, int visibleOffset) {
		StyledText textWidget= fViewer.getTextWidget();
		
		// if the position that is about to be drawn was deleted then we can't
		if (position.isDeleted())
			return;		
		
		int delta= position.offset - visibleOffset;
		if (0 <= delta && delta <= fViewer.getVisibleRegion().getLength()) {
			Point upperLeft= textWidget.getLocationAtOffset(delta);
			int width= textWidget.getClientArea().width + textWidget.getHorizontalPixel();
			int height= textWidget.getLineHeight();
			textWidget.redraw(upperLeft.x, upperLeft.y, width, height, false);
		}
	}

	/*
	 * @see IPainter#deactivate(boolean)
	 */
	public void deactivate(boolean redraw) {
		if (fIsActive) {
			fIsActive= false;
			
			/* on turning off the feature one has to paint the currently 
			 * highlighted line with the standard background color 
			 */
			if (redraw)
				drawHighlightLine(fCurrentLine, fViewer.getVisibleRegion().getOffset());
				
			fViewer.getTextWidget().removeLineBackgroundListener(this);
			
			if (fPositionManager != null)
				fPositionManager.removeManagedPosition(fCurrentLine);
				
			fLastLineNumber= -1;
		}
	}

	/*
	 * @see IPainter#dispose()
	 */
	public void dispose() {
	}

	/*
	 * @see IPainter#paint(int)
	 */
	public void paint(int reason) {
		
		// check selection
		Point selection= fViewer.getTextWidget().getSelectionRange();
		if (selection.y > 0) {
			deactivate(true);
			return;
		}
		
		// initialization
		if (!fIsActive) {
			fViewer.getTextWidget().addLineBackgroundListener(this);
			fPositionManager.addManagedPosition(fCurrentLine);
			fIsActive= true;
		}
		
		//redraw line highlight only if it hasn't been drawn yet on the respective line
		if (updateHighlightLine()) {
			// used to handle segmented view of source files
			int visibleRegionOffset= fViewer.getVisibleRegion().getOffset();
			// clear last line
			drawHighlightLine(fLastLine, visibleRegionOffset);
			// draw new line
			drawHighlightLine(fCurrentLine, visibleRegionOffset);
		}
	}

	/*
	 * @see IPainter#setPositionManager(IPositionManager)
	 */
	public void setPositionManager(IPositionManager manager) {
		fPositionManager = manager;
	}
}