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
package org.eclipse.jdt.internal.corext.dom;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.textmanipulation.MoveSourceEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEditCopier;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;
import org.eclipse.jdt.internal.corext.util.Strings;

/**
 * Move source that changes the indention of the moved range.
  */
public final class MoveIndentedSourceEdit extends MoveSourceEdit {

	private String fDestinationIndent;
	private int fSourceIndentLevel;
	private int fTabWidth;

	public MoveIndentedSourceEdit(int offset, int length) {
		super(offset, length);
		initialize(-1, "", 4); //$NON-NLS-1$
	}

	public void initialize(int sourceIndentLevel, String destIndentString, int tabWidth) {
		fSourceIndentLevel= sourceIndentLevel;
		fDestinationIndent= destIndentString;
		fTabWidth= tabWidth;
	}
	
	public boolean isInitialized() {
		return fSourceIndentLevel != -1;
	}

	/* non Java-doc
	 * @see TextEdit#copy
	 */	
	protected TextEdit copy0(TextEditCopier copier) {
		TextRange range= getTextRange();
		MoveIndentedSourceEdit result= new MoveIndentedSourceEdit(range.getOffset(), range.getLength());
		result.initialize(fSourceIndentLevel, fDestinationIndent, fTabWidth);
		return result;
	}

	protected String computeContent(TextBuffer buffer) {
		Assert.isTrue(isInitialized(), "MoveIndentedSourceEdit never initialized"); //$NON-NLS-1$
		
		String str= super.computeContent(buffer); 
		
		int destIndentLevel= Strings.computeIndent(fDestinationIndent, fTabWidth);
		if (destIndentLevel == fSourceIndentLevel) {
			return str;
		}
		return Strings.changeIndent(str, fSourceIndentLevel, fTabWidth, fDestinationIndent, buffer.getLineDelimiter());
	}
	
	public static TextEdit[] getChangeIndentEdits(TextBuffer buffer, TextRange range, int sourceIndent, int tabWidth, String destIndent) {
		int endPos= range.getExclusiveEnd();
		int firstLine= buffer.getLineOfOffset(range.getOffset());
		int lastLine= buffer.getLineOfOffset(endPos - 1);
		
		int nLines= lastLine - firstLine;
		if (nLines <= 0) {
			return null;
		}
		TextEdit[] res= new TextEdit[nLines];
		for (int i= firstLine + 1, k= 0; i <= lastLine; i++) { // no indent for first line (contained in the formatted string)
			String line= buffer.getLineContent(i);
			int offset= buffer.getLineInformation(i).getOffset();
			int length= line.length() - Strings.trimIndent(line, sourceIndent, tabWidth).length();
			if (offset + length > endPos) {
				length= endPos - offset;
			}
			res[k++]= SimpleTextEdit.createReplace(offset, length, destIndent);
		}
		return res;	
	}
	
	
}
