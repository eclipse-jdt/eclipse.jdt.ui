/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.Strings;

public class CodeBlock {
	
	private List fLines;
	
	public CodeBlock(TextBuffer buffer, int offset, int length) {
		String[] lines= buffer.convertIntoLines(offset, length, false);
		if (lines.length > 0) {
			final int lineOffset= buffer.getLineInformationOfOffset(offset).getOffset();
			if (lineOffset != offset) {
				lines[0]= CodeFormatterUtil.createIndentString(buffer.getContent(lineOffset, offset - lineOffset)) + lines[0];
			}
		}
		Strings.trimIndentation(lines, CodeFormatterUtil.getTabWidth());
		fLines= new ArrayList(Arrays.asList(lines));
	}

	public String get(int index) {
		return (String)fLines.get(index);
	}
	
	public void set(int index, String line) {
		fLines.set(index, line);
	}	
	
	public void add(int index, String line) {
		fLines.add(index, line);
	}

	public void prepend(String line) {
		fLines.add(0, line);
	}

	public void append(String line) {
		fLines.add(line);
	}

	public void prependToLine(int index, String prefix) {
		fLines.set(index, prefix + get(index));
	}
	
	public void appendToLine(int index, String appendix) {
		fLines.set(index, get(index) + appendix);
	}
	
	public int size() {
		return fLines.size();
	}
	
	public String[] getLines(int indent) {
		String is= indent > 0 ? CodeFormatterUtil.createIndentString(indent) : null;
		String[] result= new String[fLines.size()];
		for (int i= 0; i < result.length; i++) {
			result[i]= is != null ? is + get(i) : get(i);
		}
		return result;
	}
	
	public List lines() {
		return fLines;
	}
	
	public void fill(StringBuffer buffer, int indent, String delimiter, boolean delimiterForLastLine) {
		fill(buffer, CodeFormatterUtil.createIndentString(indent), delimiter, delimiterForLastLine);
	}
	public void fill(StringBuffer buffer, String indent, String delimiter, boolean delimiterForLastLine) {
		final int size= fLines.size();
		final int lastLine= size - 1;
		for (int i= 0; i < size; i++) {
			String line= (String)fLines.get(i);
			buffer.append(indent);
			buffer.append(line);
			if (i < lastLine || delimiterForLastLine)
				buffer.append(delimiter);
		}
	}
}
