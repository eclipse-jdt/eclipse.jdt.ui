/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.text;

import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.compiler.parser.InvalidInputException;
import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.compiler.parser.TerminalSymbols;
import org.eclipse.jdt.internal.core.refactoring.Assert;

public class ChangeVisibilityChange extends SimpleReplaceTextChange {

	private IMember fMember;
	
	public ChangeVisibilityChange(IMember member, String visibility) {
		super("Change visibility to " + visibility);
		fMember= member;
		Assert.isNotNull(fMember);
		setText(visibility);
	}

	protected SimpleTextChange[] adjust(ITextBuffer buffer) throws JavaModelException {
		ISourceRange range= fMember.getSourceRange();
		Scanner scanner= new Scanner();
		scanner.setSourceBuffer(buffer.getContent(range.getOffset(), range.getLength()).toCharArray());
		int offset= range.getOffset();
		int length= 0;
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
		setOffset(offset);
		setLength(length);
		return null;
	}
}

