package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.swt.custom.LineBackgroundEvent;
import org.eclipse.swt.custom.LineBackgroundListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.StyledTextContent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;

import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.jdt.core.dom.CatchClause;

import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.ui.JavaPlugin;



public class LinePainter implements IPainter, LineBackgroundListener {
	
	private StyledText fTextWidget;
	private Color fHighlightColor;
	private int fLineNumber;
	private int[] fLine= { -1, -1 };
	private boolean fIsActive= false;
	
	
	public LinePainter(ISourceViewer sourceViewer) {
		fTextWidget= sourceViewer.getTextWidget();
	}
	
	public void setHighlightColor(Color highlightColor) {
		fHighlightColor= highlightColor;
	}
	
	/*
	 * @see LineBackgroundListener#lineGetBackground(LineBackgroundEvent)
	 */
	public void lineGetBackground(LineBackgroundEvent event) {
		/* Don't use cached line information because of patched redrawing events. */
		
		int caret= fTextWidget.getCaretOffset();
		int length= event.lineText.length();
		
		if (event.lineOffset <= caret && caret <= event.lineOffset + length && fIsActive)
			event.lineBackground= fHighlightColor;
		else
			event.lineBackground= fTextWidget.getBackground();
	}
	
	private void updateHighlightLine() {
		StyledTextContent content= fTextWidget.getContent();
		fLineNumber= content.getLineAtOffset(fTextWidget.getCaretOffset());
		
		fLine[0]= content.getOffsetAtLine(fLineNumber);
			
		if (false) {
			fLine[1]= content.getLine(fLineNumber).length();
		} else {
			try {
				fLine[1]= content.getOffsetAtLine(fLineNumber + 1) - fLine[0];
			} catch (IllegalArgumentException x) {
				fLine[1]= -1;
			}
		}
	}
	
	private void drawHighlightLine() {
		if (fLine[1] == -1 || (fLineNumber + 1) == fTextWidget.getLineCount()) {
			Point upperLeft= fTextWidget.getLocationAtOffset(fLine[0]);
			int width= fTextWidget.getClientArea().width;
			int height= fTextWidget.getLineHeight();			
			fTextWidget.redraw(upperLeft.x, upperLeft.y, width, height, false);
		} else {
			fTextWidget.redrawRange(fLine[0], fLine[1], true);
		}
	}
	
	/*
	 * @see IPainter#deactivate(boolean)
	 */
	public void deactivate(boolean redraw) {
		if (fIsActive) {
			fIsActive= false;
			fTextWidget.removeLineBackgroundListener(this);
			if (redraw)
				drawHighlightLine();
		}
	}
	
	/*
	 * @see IPainter#dispose()
	 */
	public void dispose() {
	}
	
	/*
	 * @see IPainter#paint()
	 */
	public void paint() {
		if (!fIsActive) {
			fIsActive= true;
			fTextWidget.addLineBackgroundListener(this);
		}
		
		if (fLine[0] != -1 || fLine[1] != -1)
			drawHighlightLine();
		
		updateHighlightLine();
		
		drawHighlightLine();	
	}
	
	/*
	 * @see IPainter#setPositionManager(IPositionManager)
	 */
	public void setPositionManager(IPositionManager manager) {
	}
}
