/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.viewsupport;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.TextStyle;

/**
 * Represents a string with styled ranges. All ranges mark substrings of the string and do not overlap.
 * Styles are represented by {@link Style}.
 * 
 * The styled string can be modified:
 * <ul>
 * <li>new strings with styles can be appended</li>
 * <li>styles can by applied to to the existing string</li>
 * </ul>
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 *  
 */
public class ColoredString {
	
	/**
	 * Represents a style that can be associated to one ore more ranges in the {@link ColoredString}
	 *  
	 */
	public static abstract class Style {
		
		/**
		 * Applies the styles represented by this object to the given textStyle.
		 * 
		 * @param textStyle the {@link TextStyle} to modify
		 */
		public abstract void applyStyles(TextStyle textStyle);
	}
	
	private static final StyleRange[] EMPTY= new StyleRange[0];
	
	private StringBuffer fBuffer;
	private StyleRunList fStyleRuns;
	
	/**
	 * Creates an empty {@link ColoredString}.
	 */
	public ColoredString() {
		fBuffer= new StringBuffer();
		fStyleRuns= null;
	}
	
	/**
	 * Creates an {@link ColoredString} initialized with a string without a style associated.
	 * 
	 * @param string the string 
	 */
	public ColoredString(String string) {
		this(string, null);
	}
	
	/**
	 * Creates an {@link ColoredString} initialized with a string and a style.  
	 * 
	 * @param string the string 
	 * @param style the style of the text or <code>null</code> to not associated a style.
	 */
	public ColoredString(String string, Style style) {
		this();
		append(string, style);
	}
	
	/**
	 * Returns the string of this {@link ColoredString}.
	 * 
	 * @return the current string of this {@link ColoredString}.
	 */
	public String getString() {
		return fBuffer.toString();
	}
	
	/**
	 * Returns the length of the string of this {@link ColoredString}.
	 * 
	 * @return the length of the current string
	 */
	public int length() {
		return fBuffer.length();
	}
	
	/**
	 * Appends a string to the {@link ColoredString}. The appended string will have no style associated.
	 * 
	 * @param string the string to append.
	 * @return returns a reference to this object.
	 */
	public ColoredString append(String string) {
		return append(string, null);
	}
	
	/**
	 * Appends a character to the {@link ColoredString}. The appended character will have no style associated.
	 * 
	 * @param ch the character to append.
	 * @return returns a reference to this object.
	 */
	public ColoredString append(char ch) {
		return append(String.valueOf(ch), null);
	}
	
	/**
	 * Appends a string with styles to the {@link ColoredString}.
	 * 
	 * @param string the string to append.
	 * @return returns a reference to this object.
	 */
	public ColoredString append(ColoredString string) {
		if (string.length() == 0) {
			return this;
		}
		
		int offset= fBuffer.length();
		fBuffer.append(string.getString());
		
		List otherRuns= string.fStyleRuns;
		if (otherRuns != null && !otherRuns.isEmpty()) {
			for (int i= 0; i < otherRuns.size(); i++) {
				StyleRun curr= (StyleRun) otherRuns.get(i);
				if (i == 0 && curr.offset != 0) {
					appendStyleRun(null, offset); // appended string will start with the default color
				}
				appendStyleRun(curr.style, offset + curr.offset);
			}
		} else {
			appendStyleRun(null, offset); // appended string will start with the default color
		}
		return this;
	}
	
	/**
	 * Appends a character with a style to the {@link ColoredString}. The appended character will
	 * have the given style associated.
	 * 
	 * @param ch the character to append.
	 * @param style the style to of the character to append or <code>null</code> if no style should be
	 * associated to the string.
	 * @return returns a reference to this object.
	 */
	public ColoredString append(char ch, Style style) {
		return append(String.valueOf(ch), style);
	}
	
	/**
	 * Appends a string with a style to the {@link ColoredString}. The appended string will
	 * have the given style associated.
	 * 
	 * @param string the string to append.
	 * @param style the style to of the string to append or <code>null</code> if no style should be
	 * associated to the string.
	 * @return returns a reference to this object.
	 */
	public ColoredString append(String string, Style style) {
		if (string.length() == 0)
			return this;
		
		int offset= fBuffer.length(); // the length before appending
		fBuffer.append(string);
		appendStyleRun(style, offset);
		return this;
	}
	
	/**
	 * Sets a style to the given source range. The range must be subrange of actual string of this {@link ColoredString}.
	 * Styles previously set for that range will be overwritten.
	 * 
	 * @param offset the start offset of the range
	 * @param length the length of the range
	 * @param style the style to set
	 * 
	 * @throws StringIndexOutOfBoundsException if <code>start</code> is
     *             less than zero, or if offset plus length is greater than the length of this object.
	 */
	public void setStyle(int offset, int length, Style style) {
		if (offset < 0 || offset + length > fBuffer.length()) {
			throw new StringIndexOutOfBoundsException("Invalid offset (" + offset + ") or length (" + length + ")");   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
		}
		if (length == 0) {
			return;
		}
		if (!hasRuns() || getLastRun().offset <= offset) {
			appendStyleRun(style, offset);
			if (offset + length != fBuffer.length()) {
				appendStyleRun(null, offset + length);
			}
			return;
		}
		
		int endRun= findRun(offset + length);
		if (endRun >= 0) {
			// run with the same end index, nothing to change
		} else {
			endRun= -(endRun + 1);
			if (offset + length < fBuffer.length()) {
				Style prevStyle= endRun > 0 ? fStyleRuns.getRun(endRun - 1).style : null;
				fStyleRuns.add(endRun, new StyleRun(offset + length, prevStyle));
			}
		}
		
		int startRun= findRun(offset);
		if (startRun >= 0) {
			// run with the same start index
			StyleRun styleRun= fStyleRuns.getRun(startRun);
			styleRun.style= style;
		} else {
			startRun= -(startRun + 1);
			
			Style prevStyle= startRun > 0 ? fStyleRuns.getRun(startRun - 1).style : null;
			if (isDifferentStyle(prevStyle, style) || (startRun == 0 && style != null)) {
				fStyleRuns.add(startRun, new StyleRun(offset, style));
				endRun++; // endrun is moved one back
			} else {
				startRun--; // we use the previous
			}
		}
		if (startRun + 1 < endRun) {
			fStyleRuns.removeRange(startRun + 1, endRun);
		}
	}
	
	/**
	 * Returns {@link StyleRange} for all applied styles to this string
	 * 
	 * @return an array of all {@link StyleRange} applied to this string.
	 */
	public StyleRange[] getStyleRanges() {
		if (hasRuns()) {
			ArrayList res= new ArrayList();
			
			List styleRuns= getStyleRuns();
			int offset= 0;
			Style style= null;
			for (int i= 0; i < styleRuns.size(); i++) {
				StyleRun curr= (StyleRun) styleRuns.get(i);
				if (isDifferentStyle(curr.style, style)) {
					if (curr.offset > offset && style != null) {
						res.add(createStyleRange(offset, curr.offset, style));
					}
					offset= curr.offset;
					style= curr.style;
				}
			}
			if (fBuffer.length() > offset && style != null) {
				res.add(createStyleRange(offset, fBuffer.length(), style));
			}
			return (StyleRange[]) res.toArray(new StyleRange[res.size()]);
		}
		return EMPTY;
	}
	
	private int findRun(int offset) {
		// method assumes that fStyleRuns is not null
		int low= 0;
		int high= fStyleRuns.size() - 1;
		while (low <= high) {
		    int mid = (low + high) / 2;
		    StyleRun styleRun= fStyleRuns.getRun(mid);
		    if (styleRun.offset < offset) {
		    	low = mid + 1;
		    } else if (styleRun.offset > offset) {
		    	high = mid - 1;
		    } else {
		    	return mid; // key found
		    }
		}
		return -(low + 1);  // key not found.
	}
	
	private StyleRange createStyleRange(int start, int end, Style style) {
		StyleRange styleRange= new StyleRange();
		styleRange.start= start;
		styleRange.length= end - start;
		style.applyStyles(styleRange);
		return styleRange;
	}
	
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return fBuffer.toString();
	}
	
	private boolean hasRuns() {
		return fStyleRuns != null && !fStyleRuns.isEmpty();
	}
		
	private void appendStyleRun(Style style, int offset) {
		StyleRun lastRun= getLastRun();
		if (lastRun != null && lastRun.offset == offset) {
			lastRun.style= style;
			return;
		}
		
		if (lastRun == null && style != null || lastRun != null && isDifferentStyle(style, lastRun.style)) {
			getStyleRuns().add(new StyleRun(offset, style));
		}
	}
	
	private boolean isDifferentStyle(Style style1, Style style2) {
		if (style1 == null) {
			return style2 != null;
		}
		return !style1.equals(style2);
	}
	
	private StyleRun getLastRun() {
		if (fStyleRuns == null || fStyleRuns.isEmpty()) {
			return null;
		}
		return fStyleRuns.getRun(fStyleRuns.size() - 1);
	}
	
	private List getStyleRuns() {
		if (fStyleRuns == null)
			fStyleRuns= new StyleRunList();
		return fStyleRuns;
	}
	
	private static class StyleRun {
		public int offset;
		public Style style;
		
		public StyleRun(int offset, Style style) {
			this.offset= offset;
			this.style= style;
		}
		
		public String toString() {
			return "Offset " + offset + ", style: " + style.toString();  //$NON-NLS-1$//$NON-NLS-2$
		}
	}
	
	private static class StyleRunList extends ArrayList {
		private static final long serialVersionUID= 123L;

		public StyleRunList() {
			super(3);
		}

		public StyleRun getRun(int index) {
			return (StyleRun) get(index);
		}
				
		public void removeRange(int fromIndex, int toIndex) {
			super.removeRange(fromIndex, toIndex);
		}	
	}
}
