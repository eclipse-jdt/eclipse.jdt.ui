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

import java.util.List;

import org.eclipse.core.runtime.CoreException;

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
	private SimpleTextEdit fIndentEdit;
	private boolean fIndentCorrection;

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
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.textmanipulation.MoveSourceEdit#perform(org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer)
	 */
	public void perform(TextBuffer buffer) throws CoreException {
		SimpleTextEdit[] edits= getChangeIndentEdits(buffer, getTextRange());
		fIndentCorrection= true;
		for (int i= edits.length - 1; i >= 0; i--) {
			fIndentEdit= edits[i];
			buffer.replace(fIndentEdit.getTextRange(), fIndentEdit.getText());
		}
		fIndentCorrection= false;
		super.perform(buffer);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.textmanipulation.MoveSourceEdit#updateTextRange(int, java.util.List)
	 */
	protected void updateTextRange(int delta, List executedEdits) {
		if (fIndentCorrection) {
			adjustLength(delta);
			updateParents(delta);
			TextRange indentRange= fIndentEdit.getTextRange();
			int indentOffest= indentRange.getOffset();
			int size= executedEdits.size();
			for (int i= 0; i < size; i++) {
				TextEdit edit= (TextEdit)executedEdits.get(i);
				TextRange range= edit.getTextRange();
				if (range.liesBehind(indentRange)) {
					edit.adjustOffset(delta);
				} else if (indentRange.covers(range)) {
					edit.markAsDeleted();
				} else if (range.covers(indentOffest)) {
					edit.adjustLength(range.getExclusiveEnd() - indentOffest);
				} else if (range.covers(indentRange.getInclusiveEnd())) {
					edit.adjustLength(indentRange.getExclusiveEnd() - range.getOffset());
				}
			}
		} else {
			super.updateTextRange(delta, executedEdits);
		}
	}
	
	private SimpleTextEdit[] getChangeIndentEdits(TextBuffer buffer, TextRange range) {
		return getChangeIndentEdits(buffer, range, fSourceIndentLevel, fTabWidth, fDestinationIndent);
	}
	
	public static SimpleTextEdit[] getChangeIndentEdits(TextBuffer buffer, TextRange range, int sourceIndent, int tabWidth, String destIndent) {
		int endPos= range.getExclusiveEnd();
		int firstLine= buffer.getLineOfOffset(range.getOffset());
		int lastLine= buffer.getLineOfOffset(endPos - 1);
		
		int nLines= lastLine - firstLine;
		if (nLines <= 0) {
			return new SimpleTextEdit[0];
		}
		SimpleTextEdit[] res= new SimpleTextEdit[nLines];
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
