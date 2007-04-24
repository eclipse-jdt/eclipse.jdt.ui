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
import java.util.Iterator;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;


public class ColoredString {
	
	public static abstract class Style {
		public abstract Color getForeground(Display display);
	}
	
	public static final Style DEFAULT_STYLE= null;
	
	private StringBuffer fBuffer;
	private ArrayList fRanges;
	
	public ColoredString() {
		fBuffer= new StringBuffer();
		fRanges= new ArrayList(2);
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
		return fRanges.iterator();
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
			StyleRange curr= (StyleRange) iterator.next();
			fRanges.add(new StyleRange(offset + curr.offset, curr.length, curr.style));
		}
		return this;
	}
		
	public ColoredString append(String text, Style style) {
		if (text.length() == 0)
			return this;
		
		int offset= fBuffer.length();
		fBuffer.append(text);
		if (style != null) {
			if (!fRanges.isEmpty()) {
				StyleRange last= (StyleRange) fRanges.get(fRanges.size() - 1);
				if (last.offset + last.length == offset && style.equals(last.style)) {
					last.length += text.length();
					return this;
				}
			}
			fRanges.add(new StyleRange(offset, text.length(), style));
		}
		return this;
	}
	
	public void colorize(int offset, int length, Style style) {
		if (offset < 0 || offset + length > fBuffer.length()) {
			throw new IllegalArgumentException("Invalid offset (" + offset + ") or length (" + length + ")");   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
		}
		
		
		int insertPos= 0;
		for (int i= 0; i < fRanges.size(); i++) {
			StyleRange curr= (StyleRange) fRanges.get(i);
			if (curr.offset + curr.length <= offset) {
				insertPos= i + 1;
			}
		}
		if (insertPos < fRanges.size()) {
			StyleRange curr= (StyleRange) fRanges.get(insertPos);
			if (curr.offset > offset + length) {
				throw new IllegalArgumentException("Overlapping ranges"); //$NON-NLS-1$
			}
		}
		fRanges.add(insertPos, new StyleRange(offset, length, style));
	}
	
	public static class StyleRange {
		public int offset;
		public int length;
		public Style style;
		
		public StyleRange(int offset, int length, Style style) {
			this.offset= offset;
			this.length= length;
			this.style= style;
		}
	}
}
