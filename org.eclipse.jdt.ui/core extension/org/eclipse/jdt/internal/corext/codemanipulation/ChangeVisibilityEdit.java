/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.codemanipulation;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.compiler.parser.InvalidInputException;
import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.compiler.parser.TerminalSymbols;
import org.eclipse.jdt.internal.core.Assert;

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
	 * @see TextEdit#getModifiedLanguageElement
	 */
	public Object getModifiedLanguageElement() {
		return fMember;
	}
	
	/* non Java-doc
	 * @see TextEdit#connect
	 */
	public void connect(TextBuffer buffer) throws CoreException {
		int offset= fMember.getSourceRange().getOffset();
		int length= 0;
		Scanner scanner= new Scanner();
		scanner.setSourceBuffer(buffer.getContent(offset, fMember.getSourceRange().getLength()).toCharArray());
		int token= 0;
		try {
			while((token= scanner.getNextToken()) != TerminalSymbols.TokenNameEOF) {
				if (token == TerminalSymbols.TokenNamepublic || token == TerminalSymbols.TokenNameprotected || token == TerminalSymbols.TokenNameprivate) {
					offset+= scanner.startPosition;
					length= scanner.currentPosition - scanner.startPosition;
					break;
				}
			}
		} catch (InvalidInputException e) {
			throw new JavaModelException(e, IJavaModelStatusConstants.INVALID_CONTENTS);
		}
		String text= fVisibility;
		if (length == 0)
			text+= " ";
		setPosition(new TextPosition(offset, length));
		setText(text);
		super.connect(buffer);
	}	
}

