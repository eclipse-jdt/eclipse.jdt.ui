package org.eclipse.jdt.internal.ui.text;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.text.HoverTextControl;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.HoverTextControl.IHoverInformationPresenter;

import org.eclipse.jdt.internal.ui.JavaPlugin;



public class LineWrappingTextPresenter implements HoverTextControl.IHoverInformationPresenter {
	
	private static final String LINE_DELIM= System.getProperty("line.separator", "\n");
	
	
	public LineWrappingTextPresenter() {
		super();
	}
	
	protected Reader createReader(String hoverInfo) {
		return new HTML2TextReader(new StringReader(hoverInfo));
	}
	
	/*
	 * @see IHoverInformationPresenter#updatePresentation(Display display, String, TextPresentation, int, int)
	 */
	public String updatePresentation(Display display, String hoverInfo, TextPresentation presentation, int maxWidth, int maxHeight) {
		
		if (hoverInfo == null)
			return null;
			
		GC gc= new GC(display);
		try {
			
			StringBuffer buf= new StringBuffer();
			int maxNumberOfLines= Math.round(maxHeight / gc.getFontMetrics().getHeight());
			
			LineBreakingReader reader= new LineBreakingReader(createReader(hoverInfo), gc, maxWidth);
			
			String line= reader.readLine();
			while (maxNumberOfLines > 0 && line != null) {
				
				if (buf.length() != 0)
					buf.append(LINE_DELIM);
					
				buf.append(' '); // add one space indent
				buf.append(line);
				line= reader.readLine();
				maxNumberOfLines--;
			}
			
			if (line != null) {
				buf.append(LINE_DELIM);
				buf.append("...");
			}
			
			return trim(buf);
			
		} catch (IOException e) {
			
			JavaPlugin.log(e);
			return null;
			
		} finally {
			gc.dispose();
		}
	}
	
	private String trim(StringBuffer buffer) {
		
		int length= buffer.length();
				
		int end= length -1;
		while (end >= 0 && Character.isWhitespace(buffer.charAt(end)))
			-- end;
		
		if (end == -1)
			return "";
			
		if (end < length -1)
			buffer.delete(end, length);
		else
			end= length;
			
		int start= 0;
		while (start < end && Character.isWhitespace(buffer.charAt(start)))
			++ start;
			
		if (start > 0)
			return buffer.substring(start);
			
		return buffer.toString();
	}
}

