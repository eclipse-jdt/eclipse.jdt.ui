/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.text;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.TextUtilities;
import org.eclipse.jdt.internal.formatter.CodeFormatter;

public class AddMemberChange extends SimpleReplaceTextChange {
	
	public static final int INSERT_BEFORE= 0;
	public static final int INSERT_AFTER= 1;
	
	private IMember fSibling;
	private int fInsertionKind;
	private String[] fSource;
	private int fTabWidth;
	
	public AddMemberChange(String name, IMember sibling, int insertionKind, String[] source, int tabWidth) {
		super(name);
		fSibling= sibling;
		Assert.isNotNull(fSibling);
		fInsertionKind= insertionKind;
		Assert.isTrue(fInsertionKind == INSERT_BEFORE || fInsertionKind == INSERT_AFTER);
		fSource= source;
		Assert.isNotNull(fSource);
		fTabWidth= tabWidth;
		Assert.isTrue(fTabWidth >= 0);
	}
	
	public IJavaElement getCorrespondingJavaElement() {
		return fSibling.getDeclaringType();
	}
	
	protected SimpleTextChange[] adjust(ITextBuffer buffer) throws JavaModelException {
		StringBuffer sb= new StringBuffer();
		String lineDelimiter= buffer.getLineDelimiter();
		int offset= computeOffset(buffer);
		setOffset(offset);
		int indent = createLineIndent(buffer);
		if (fInsertionKind == INSERT_AFTER) {
			sb.append(lineDelimiter);
			sb.append(lineDelimiter);
		}
		sb.append(getFormattedSource(indent, lineDelimiter));
		if (fInsertionKind == INSERT_BEFORE) {
			sb.append(lineDelimiter);
			sb.append(lineDelimiter);
		}
		String text= sb.toString();
		setLength(0);
		setText(text);
		return null;
	}

	private int createLineIndent(ITextBuffer buffer) throws JavaModelException {
		int line= buffer.getLineOfOffset(fSibling.getSourceRange().getOffset());
		return buffer.getLineIndent(line, fTabWidth);
	}
	
	private int computeOffset(ITextBuffer buffer) throws JavaModelException {
		ISourceRange range= fSibling.getSourceRange();
		int end= range.getOffset() + range.getLength();
		if (fInsertionKind == INSERT_AFTER) {
			return end;
		} else {
			int line= buffer.getLineOfOffset(range.getOffset());
			ITextRegion region= buffer.getLineInformation(line);
			return region.getOffset();
		}
	}
	
	private String getFormattedSource(int initialIndentationLevel, String lineDelimiter) {
		StringBuffer buffer= new StringBuffer();
		int last= fSource.length - 1;
		for (int i= 0; i < fSource.length; i++) {
			buffer.append(fSource[i]);
			if (i < last)
				buffer.append(lineDelimiter);
		}		
		CodeFormatter formatter= new CodeFormatter(JavaCore.getOptions());
		formatter.options.setLineSeparator(lineDelimiter);
		formatter.setInitialIndentationLevel(initialIndentationLevel);
		return formatter.formatSourceString(buffer.toString());
	}	
}

