/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.codemanipulation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.Strings;

public class CodeBlock extends AbstractCodeBlock {
	
	private List fLines;
	
	public CodeBlock(TextBuffer buffer) {
		this(buffer, 0, buffer.getLength());
	}
	
	public CodeBlock(TextBuffer buffer, int offset, int length) {
		String[] lines= buffer.convertIntoLines(offset, length, false);
		if (lines.length > 0) {
			final int lineOffset= buffer.getLineInformationOfOffset(offset).getOffset();
			if (lineOffset != offset) {
				lines[0]= CodeFormatterUtil.createIndentString(buffer.getContent(lineOffset, offset - lineOffset)) + lines[0];
			}
		}
		initialize(lines);
	}
	
	public CodeBlock(String code) {
		initialize(Strings.convertIntoLines(code));
	}
	
	private void initialize(String[] lines) {
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
	
	public List lines() {
		return fLines;
	}

	public boolean isEmpty() {
		return fLines.isEmpty();
	}

	public void fill(StringBuffer buffer, String firstLineIndent, String indent, String lineSeparator) {
		int size= fLines.size();
		int lastLine= size - 1;
		for (int i= 0; i < size; i++) {
			if (i == 0)
				buffer.append(firstLineIndent);
			else
				buffer.append(indent);
			buffer.append((String)fLines.get(i));
			if (i < lastLine)
				buffer.append(lineSeparator);
		}
	}
}
