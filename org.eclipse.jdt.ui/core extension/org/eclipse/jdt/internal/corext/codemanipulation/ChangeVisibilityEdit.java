/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.codemanipulation;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;

import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBufferEditor;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;

/**
 * Changes the visibility of a <code>IMember</code> instance.
 */
public class ChangeVisibilityEdit extends SimpleTextEdit {

	private IMember fMember;
	private String fVisibility;

	/**
	 * Create a new text edit that changes the visibility of the given member.
	 * 
	 * @param member the member for which the visibility is to be changed
	 * @param visibility the new visibility
	 */	
	public ChangeVisibilityEdit(IMember member, String visibility) {
		fMember= member;
		Assert.isNotNull(fMember);
		fVisibility= visibility;
		Assert.isNotNull(fVisibility);
	}
	
	/* non Java-doc
	 * @see TextEdit#getCopy
	 */
	public TextEdit copy() {
		return new ChangeVisibilityEdit(fMember, fVisibility);
	}
	
	/* non Java-doc
	 * @see TextEdit#getModifiedElement
	 */
	public Object getModifiedElement() {
		return fMember;
	}
	
	/* non Java-doc
	 * @see TextEdit#connect
	 */
	public void connect(TextBufferEditor editor) throws CoreException {
		TextBuffer buffer= editor.getTextBuffer();
		int offset= fMember.getSourceRange().getOffset();
		int length= 0;
		IScanner scanner= ToolFactory.createScanner(false, false, false, false);
		scanner.setSource(buffer.getContent(offset, fMember.getSourceRange().getLength()).toCharArray());
		int token= 0;
		try {
			while((token= scanner.getNextToken()) != ITerminalSymbols.TokenNameEOF) {
				if (token == ITerminalSymbols.TokenNamepublic || token == ITerminalSymbols.TokenNameprotected || token == ITerminalSymbols.TokenNameprivate) {
					offset+= scanner.getCurrentTokenStartPosition();
					length= scanner.getCurrentTokenEndPosition() - scanner.getCurrentTokenStartPosition() + 1;
					break;
				}
			}
		} catch (InvalidInputException e) {
			throw new JavaModelException(e, IJavaModelStatusConstants.INVALID_CONTENTS);
		}
		String text= fVisibility;
		if (length == 0)
			text+= " "; //$NON-NLS-1$
		setTextRange(new TextRange(offset, length));
		setText(text);
		super.connect(editor);
	}	
}

