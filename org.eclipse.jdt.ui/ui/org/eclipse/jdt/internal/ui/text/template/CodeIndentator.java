/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.text.template;

import org.eclipse.jdt.internal.core.refactoring.TextUtilities;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultLineTracker;
import org.eclipse.jface.text.GapTextStore;
import org.eclipse.jface.text.ILineTracker;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextStore;

/**
 * The code indentator indentates a text block by a given indentation level.
 * The interface is similar to <code>CodeFormatter</code>.
 */
public class CodeIndentator {

	private int fIndentationLevel= 0;
	private int[] fPositions = new int[0];

	/**
	 * Sets the indentation level.
	 */
	public void setIndentationLevel(int indentationLevel) {
		fIndentationLevel= indentationLevel;
	}
	
	/**
	 * Sets the positions to map.
	 */
	public void setPositionsToMap(int[] positions) {
		fPositions= positions;
	}
	
	/**
	 * Returns the mapped positions. Should be called after an indentation operation.
	 */
	public int[] getMappedPositions() {
		return fPositions;
	}

	/**
	 * Indentates a string.
	 */
	public String indentate(String string) {		
		StringBuffer buffer = new StringBuffer(string.length());
		
		String indentation= TextUtilities.createIndentString(fIndentationLevel);
		String[] lines= splitIntoLines(string);

		int[] positionDelta= new int[fPositions.length];
		
		int position= 0;
		for (int i= 0; i != lines.length; i++) {
			buffer.append(indentation);
			buffer.append(lines[i]);
			
			for (int j= 0; j != fPositions.length; j++)		
				if (fPositions[j] >= position)
					positionDelta[j] += indentation.length();
			
			position += lines[i].length();
		}
		
		for (int i= 0; i != fPositions.length; i++)
			fPositions[i] += positionDelta[i];
		
		return buffer.toString();		
	}
	
	private static String[] splitIntoLines(String text) {
		try {
			ITextStore store= new GapTextStore(0, 1);
			store.set(text);
			
			ILineTracker tracker= new DefaultLineTracker();
			tracker.set(text);
			
			int size= tracker.getNumberOfLines();
			String result[]= new String[size];
			for (int i= 0; i < size; i++) {
				String lineDelimiter= null;
				try {
					lineDelimiter= tracker.getLineDelimiter(i);
				} catch (BadLocationException e) {}
				
				IRegion region= tracker.getLineInformation(i);
				
				if (lineDelimiter == null) {
					result[i]= store.get(region.getOffset(), region.getLength());
				} else {
					result[i]= store.get(region.getOffset(), region.getLength()) +
						lineDelimiter;
				}
			}
			return result;
		} catch (BadLocationException e) {
			return null;
		}
	}
	
}

