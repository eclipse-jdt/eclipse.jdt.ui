/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.codemanipulation;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.formatter.CodeFormatter;

/**
 * @deprecated Use class MemberEdit instead.
 */
public class AddMemberEdit extends SimpleTextEdit {
	
	public static final int INSERT_BEFORE= 0;
	public static final int INSERT_AFTER= 1;
	
	private IMember fSibling;
	private int fInsertionKind;
	private String[] fSource;
	private int fTabWidth;
	
	/**
	 * @deprecated Use class MemberEdit instead.
	 */
	public AddMemberEdit(IMember sibling, int insertionKind, String[] source, int tabWidth) {
		fSibling= sibling;
		Assert.isNotNull(fSibling);
		fInsertionKind= insertionKind;
		Assert.isTrue(fInsertionKind == INSERT_BEFORE || fInsertionKind == INSERT_AFTER);
		fSource= source;
		Assert.isNotNull(fSource);
		fTabWidth= tabWidth;
		Assert.isTrue(fTabWidth >= 0);
	}
	
	/* non Java-doc
	 * @see TextEdit#getCopy
	 */
	public TextEdit copy() {
		return new AddMemberEdit(fSibling, fInsertionKind, fSource, fTabWidth);
	}
	
	/* non Java-doc
	 * @see TextEdit#getModifiedLanguageElement
	 */
	public Object getModifiedLanguageElement() {
		return fSibling.getParent();
	}
	
	/* non Java-doc
	 * @see TextEdit#connect
	 */
	public void connect(TextBuffer buffer) throws CoreException {
		StringBuffer sb= new StringBuffer();
		String lineDelimiter= buffer.getLineDelimiter();
		int offset= computeOffset(buffer);
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
		setTextPosition(new TextPosition(offset, 0));
		setText(text);
		super.connect(buffer);
	}

	private int createLineIndent(TextBuffer buffer) throws CoreException {
		int line= buffer.getLineOfOffset(fSibling.getSourceRange().getOffset());
		return buffer.getLineIndent(line, fTabWidth);
	}
	
	private int computeOffset(TextBuffer buffer) throws CoreException {
		ISourceRange range= fSibling.getSourceRange();
		int end= range.getOffset() + range.getLength();
		if (fInsertionKind == INSERT_AFTER) {
			return end;
		} else {
			int line= buffer.getLineOfOffset(range.getOffset());
			TextRegion region= buffer.getLineInformation(line);
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

