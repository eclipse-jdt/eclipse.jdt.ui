package org.eclipse.jdt.internal.ui.text;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import java.util.Iterator;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextPresentation;

import org.eclipse.jdt.internal.core.util.CharArrayBuffer;
import org.eclipse.jdt.internal.ui.JavaPlugin;



public class HTMLTextPresenter implements DefaultInformationControl.IInformationPresenter {
	
	private static final String LINE_DELIM= System.getProperty("line.separator", "\n");
	
	private int fCounter;
	
	public HTMLTextPresenter() {
		super();
	}
	
	protected Reader createReader(String hoverInfo, TextPresentation presentation) {
		return new HTML2TextReader(new StringReader(hoverInfo), presentation);
	}
	
	protected void adaptTextPresentation(TextPresentation presentation, int offset, int insertLength) {
				
		int yoursStart= offset;
		int yoursEnd=   offset + insertLength -1;
		yoursEnd= Math.max(yoursStart, yoursEnd);
		
		Iterator e= presentation.getAllStyleRangeIterator();
		while (e.hasNext()) {
			
			StyleRange range= (StyleRange) e.next();
		
			int myStart= range.start;
			int myEnd=   range.start + range.length -1;
			myEnd= Math.max(myStart, myEnd);
			
			if (myEnd < yoursStart)
				continue;
			
			if (myStart < yoursStart)
				range.length += insertLength;
			else
				range.start += insertLength;
		}
	}
	
	private void append(StringBuffer buffer, String string, TextPresentation presentation) {
		
		int length= string.length();
		buffer.append(string);
		
		if (presentation != null)
			adaptTextPresentation(presentation, fCounter, length);
			
		fCounter += length;
	}
	
	private String getIndent(String line) {
		int i= 0;
		while (Character.isWhitespace(line.charAt(i))) ++ i;
		return (line.substring(0, i) + " ");
	}
	
	/*
	 * @see IHoverInformationPresenter#updatePresentation(Display display, String, TextPresentation, int, int)
	 */
	public String updatePresentation(Display display, String hoverInfo, TextPresentation presentation, int maxWidth, int maxHeight) {
		
		if (hoverInfo == null)
			return null;
			
		GC gc= new GC(display);
		try {
			
			StringBuffer buffer= new StringBuffer();
			int maxNumberOfLines= Math.round(maxHeight / gc.getFontMetrics().getHeight());
			
			fCounter= 0;
			LineBreakingReader reader= new LineBreakingReader(createReader(hoverInfo, presentation), gc, maxWidth);
			
			boolean lastLineFormatted= false;
			String lastLineIndent= null;
			
			String line=reader.readLine();
			boolean lineFormatted= reader.isFormattedLine();
			
			while (maxNumberOfLines > 0 && line != null) {
				
				if (buffer.length() > 0) {
					if (!lastLineFormatted)
						append(buffer, LINE_DELIM, null);
					else {
						append(buffer, LINE_DELIM, presentation);
						if (lastLineIndent != null)
							append(buffer, lastLineIndent, presentation);
					}
				}
				
				append(buffer, line, null);
				
				lastLineFormatted= lineFormatted;
				if (!lineFormatted)
					lastLineIndent= null;
				else if (lastLineIndent == null)
					lastLineIndent= getIndent(line);
					
				line= reader.readLine();
				lineFormatted= reader.isFormattedLine();
				
				maxNumberOfLines--;
			}
			
			if (line != null) {
				append(buffer, LINE_DELIM, lineFormatted ? presentation : null);
				append(buffer, "...", presentation);
			}
			
			return trim(buffer, presentation);
			
		} catch (IOException e) {
			
			JavaPlugin.log(e);
			return null;
			
		} finally {
			gc.dispose();
		}
	}
	
	private String trim(StringBuffer buffer, TextPresentation presentation) {
		
		int length= buffer.length();
				
		int end= length -1;
		while (end >= 0 && Character.isWhitespace(buffer.charAt(end)))
			-- end;
		
		if (end == -1)
			return "";
			
		if (end < length -1)
			buffer.delete(end + 1, length);
		else
			end= length;
			
		int start= 0;
		while (start < end && Character.isWhitespace(buffer.charAt(start)))
			++ start;
			
		buffer.delete(0, start);
		presentation.setResultWindow(new Region(start, buffer.length()));
		return buffer.toString();
	}
}

