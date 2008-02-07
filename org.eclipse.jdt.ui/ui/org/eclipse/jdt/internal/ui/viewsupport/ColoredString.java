/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.graphics.Color;

import org.eclipse.jface.resource.ColorRegistry;


public class ColoredString {
	
	public static class Style {
		private final String fForegroundColorName;
		private final String fBackgroundColorName;

		public Style(String foregroundColorName, String backgroundColorName) {
			fForegroundColorName= foregroundColorName;	
			fBackgroundColorName= backgroundColorName;
		}
		
		public String getForegroundColorName() {
			return fForegroundColorName;
		}
		
		public String getBackgroundColorName() {
			return fBackgroundColorName;
		}
		
		public Color getForegroundColor(ColorRegistry registry) {
			if (fForegroundColorName != null)
				return registry.get(fForegroundColorName);
			return null;
		}
		
		public Color getBackgroundColor(ColorRegistry registry) {
			if (fBackgroundColorName != null)
				return registry.get(fBackgroundColorName);
			return null;
		}
	}
	
	public static final Style DEFAULT_STYLE= null;
	
	private StringBuffer fBuffer;
	private ArrayList fRanges;
	
	public ColoredString() {
		fBuffer= new StringBuffer();
		fRanges= null;
	}
	
	public ColoredString(String text) {
		this(text, ColoredString.DEFAULT_STYLE);
	}
	
	public ColoredString(String text, Style style) {
		this();
		append(text, style);
	}
	
	public String getString() {
		return fBuffer.toString();
	}
	
	public int length() {
		return fBuffer.length();
	}
	
	public Iterator getRanges() {
		if (!hasRanges())
			return Collections.EMPTY_LIST.iterator();
		return getRangesList().iterator();
	}
	
	public org.eclipse.swt.custom.StyleRange[] getStyleRanges(ColorRegistry registry) {
		List ranges= getRangesList();
		org.eclipse.swt.custom.StyleRange[] res= new org.eclipse.swt.custom.StyleRange[ranges.size()];
		for (int i= 0; i < ranges.size(); i++) {
			Range curr= (Range) ranges.get(i);
			Style style= curr.style;
			res[i]= new org.eclipse.swt.custom.StyleRange(curr.offset, curr.length, style.getForegroundColor(registry), style.getBackgroundColor(registry));
		}
		return res;
	}
	
	
	
	public ColoredString append(String text) {
		return append(text, DEFAULT_STYLE);
	}
	
	public ColoredString append(char ch) {
		return append(String.valueOf(ch), DEFAULT_STYLE);
	}
	
	public ColoredString append(ColoredString string) {
		int offset= fBuffer.length();
		fBuffer.append(string.getString());
		for (Iterator iterator= string.getRanges(); iterator.hasNext();) {
			Range curr= (Range) iterator.next();
			addRange(new Range(offset + curr.offset, curr.length, curr.style));
		}
		return this;
	}
		
	public ColoredString append(String text, Style style) {
		if (text.length() == 0)
			return this;
		
		int offset= fBuffer.length();
		fBuffer.append(text);
		if (style != null) {
			int nRanges= getNumberOfRanges();
			if (nRanges > 0) {
				Range last= getRange(nRanges - 1);
				if (last.offset + last.length == offset && style.equals(last.style)) {
					last.length += text.length();
					return this;
				}
			}
			addRange(new Range(offset, text.length(), style));
		}
		return this;
	}
	
	public void colorize(int offset, int length, Style style) {
		if (offset < 0 || offset + length > fBuffer.length()) {
			throw new IllegalArgumentException("Invalid offset (" + offset + ") or length (" + length + ")");   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
		}
		
		int insertPos= 0;
		int nRanges= getNumberOfRanges();
		for (int i= 0; i < nRanges; i++) {
			Range curr= getRange(i);
			if (curr.offset + curr.length <= offset) {
				insertPos= i + 1;
			}
		}
		if (insertPos < nRanges) {
			Range curr= getRange(insertPos);
			if (curr.offset > offset + length) {
				throw new IllegalArgumentException("Overlapping ranges"); //$NON-NLS-1$
			}
		}
		addRange(insertPos, new Range(offset, length, style));
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return fBuffer.toString();
	}
	
	private boolean hasRanges() {
		return fRanges != null && !fRanges.isEmpty();
	}
	
	private int getNumberOfRanges() {
		return fRanges == null ? 0 : fRanges.size();
	}
	
	private Range getRange(int index) {
		if (fRanges != null) {
			return (Range) fRanges.get(index);
		}
		throw new IndexOutOfBoundsException();
	}
	
	private void addRange(Range range) {
		getRangesList().add(range);
	}
	
	private void addRange(int index, Range range) {
		getRangesList().add(index, range);
	}
	
	private List getRangesList() {
		if (fRanges == null)
			fRanges= new ArrayList(2);
		return fRanges;
	}
	
	public static class Range {
		public int offset;
		public int length;
		public Style style;
		
		public Range(int offset, int length, Style style) {
			this.offset= offset;
			this.length= length;
			this.style= style;
		}
	}
}
