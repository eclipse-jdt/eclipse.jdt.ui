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
package org.eclipse.jdt.internal.corext.codemanipulation;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEditCopier;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBufferEditor;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRegion;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.Strings;

public final class CodeBlockEdit extends TextEdit {

	private TextRange fRange;
	private AbstractCodeBlock fBlock;
	private int fIndent;
	private int fSpacing;

	public static CodeBlockEdit createReplace(int offset, int length, AbstractCodeBlock block, int indent) {
		return new CodeBlockEdit(new TextRange(offset, length), block, indent, 0);
	}

	public static CodeBlockEdit createReplace(int offset, int length, AbstractCodeBlock block) {
		return new CodeBlockEdit(new TextRange(offset, length), block, -1, 0);
	}
	
	public static CodeBlockEdit createInsert(int offset, AbstractCodeBlock block, int spacing) {
		return new CodeBlockEdit(new TextRange(offset, 0), block, -1, spacing);
	}

	private CodeBlockEdit(TextRange range, AbstractCodeBlock block, int indent, int spacing) {
		Assert.isNotNull(range);
		Assert.isNotNull(block);
		fRange= range;
		fBlock= block;
		fIndent= indent;
		fSpacing= spacing;
	}

	public TextEdit copy0(TextEditCopier copier) {
		return new CodeBlockEdit(fRange.copy(), fBlock, fIndent, fSpacing);
	}

	/* non Java-doc
	 * @see TextEdit#connect
	 */
	public void connect(TextBuffer buffer) throws CoreException {		
		final int offset= fRange.getOffset();
		final int end= offset + fRange.getLength();
		int lineOffset= buffer.getLineInformationOfOffset(end).getOffset();
		if (lineOffset == end) {
			int lineNumber= buffer.getLineOfOffset(lineOffset);
			if (lineNumber > 0) {
				fRange= new TextRange(offset, fRange.getLength() - buffer.getLineDelimiter(lineNumber - 1).length());
			}
		}
	}
	
	/* non Java-doc
	 * @see TextEdit#getTextRange
	 */
	public TextRange getTextRange() {
		return fRange;
	}
	
	/* non Java-doc
	 * @see TextEdit#doPerform
	 */
	public final void perform(TextBuffer buffer) throws CoreException {
		String current= buffer.getContent(fRange.getOffset(), fRange.getLength());
		buffer.replace(fRange, createText(buffer));
	}	
	
	private String createText(TextBuffer buffer) {
		int tabWidth= CodeFormatterUtil.getTabWidth();
		final int offset= fRange.getOffset();
		final int firstLine= buffer.getLineOfOffset(offset);
		final TextRegion region= buffer.getLineInformation(firstLine);
		
		if (fIndent < 0) {
			fIndent= Strings.computeIndent(buffer.getLineContent(firstLine), tabWidth);
		}
		String indent= CodeFormatterUtil.createIndentString(fIndent);
		String firstLineIndent= indent;
		
		if (fRange.getLength() == 0) {
			if (fSpacing == 0)
				firstLineIndent= ""; //$NON-NLS-1$
		} else {
			String lineContent= buffer.getContent(offset, region.getLength() - (offset - region.getOffset()));
			firstLineIndent= CodeFormatterUtil.createIndentString(Strings.computeIndent(lineContent, tabWidth));
		}
				
		String delimiter= buffer.getLineDelimiter(firstLine);
		StringBuffer result= new StringBuffer();
		for (int i= 0; i < fSpacing; i++) {
			result.append(delimiter);
		}
		fBlock.fill(result, firstLineIndent, indent, delimiter);
		return result.toString();
	}
}
