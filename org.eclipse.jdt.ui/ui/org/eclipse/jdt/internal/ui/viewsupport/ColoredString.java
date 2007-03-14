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


public class ColoredString {

	private StringBuffer fBuffer;
	private ArrayList fRanges;
	
	public ColoredString() {
		fBuffer= new StringBuffer();
		fRanges= new ArrayList(5);
	}
	
	public ColoredString(String text) {
		this(text, 0, 0);
	}
	
	public ColoredString(String text, int foregroundColor, int backgroundColor) {
		this();
		append(text, foregroundColor, backgroundColor);
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
		return append(text, 0, 0);
	}
	
	public ColoredString append(char ch) {
		return append(String.valueOf(ch), 0, 0);
	}
	
	public ColoredString append(ColoredString string) {
		int offset= fBuffer.length();
		fBuffer.append(string.getString());
		for (Iterator iterator= string.getRanges(); iterator.hasNext();) {
			ColorRange curr= (ColorRange) iterator.next();
			fRanges.add(new ColorRange(offset + curr.offset, curr.length, curr.foregroundColor, curr.backgroundColor));
		}
		return this;
	}
	
	public ColoredString append(String text, int foregroundColor) {
		return append(text, foregroundColor, 0);
	}
	
	public ColoredString append(String text, int foregroundColor, int backgroundColor) {
		if (text.length() == 0)
			return this;
		
		int offset= fBuffer.length();
		fBuffer.append(text);
		if (foregroundColor != 0 || backgroundColor != 0) {
			if (!fRanges.isEmpty()) {
				ColorRange last= (ColorRange) fRanges.get(fRanges.size() - 1);
				if (last.offset + last.length == offset && last.foregroundColor == foregroundColor && last.backgroundColor == backgroundColor) {
					last.length += text.length();
					return this;
				}
			}
			fRanges.add(new ColorRange(offset, text.length(), foregroundColor, backgroundColor));
		}
		return this;
	}
	
	public void colorize(int offset, int length, int foregroundColor, int backgroundColor) {
		if (offset < 0 || offset + length > fBuffer.length()) {
			throw new IllegalArgumentException("Invalid offset (" + offset + ") or length (" + length + ")");   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
		}
		
		
		int insertPos= 0;
		for (int i= 0; i < fRanges.size(); i++) {
			ColorRange curr= (ColorRange) fRanges.get(i);
			if (curr.offset + curr.length <= offset) {
				insertPos= i + 1;
			}
		}
		if (insertPos < fRanges.size()) {
			ColorRange curr= (ColorRange) fRanges.get(insertPos);
			if (curr.offset > offset + length) {
				throw new IllegalArgumentException("Overlapping ranges"); //$NON-NLS-1$
			}
		}
		fRanges.add(insertPos, new ColorRange(offset, length, foregroundColor, backgroundColor));
	}
	
	public static class ColorRange {
		public int offset;
		public int length;
		public int foregroundColor;
		public int backgroundColor;
		
		public ColorRange(int offset, int length, int foregroundColor, int backgroundColor) {
			this.offset= offset;
			this.length= length;
			this.foregroundColor= foregroundColor;
			this.backgroundColor= backgroundColor;
		}
	}
}
