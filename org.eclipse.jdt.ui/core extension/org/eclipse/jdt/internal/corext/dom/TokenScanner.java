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

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;

import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.ui.JavaUIStatus;

/**
 * Wraps a scanner and offers convenient methods for finding tokens 
 */
public class TokenScanner {
	
	public static final int END_OF_FILE= 20001;
	public static final int LEXICAL_ERROR= 20002;
	
	private IScanner fScanner;
	private int fEndPosition;
	
	/**
	 * Creates a TokenScanner
	 * @param scanner The scanner to be wrapped
	 */
	public TokenScanner(IScanner scanner) {
		fScanner= scanner;
		fEndPosition= fScanner.getSource().length - 1;
	}
	
	/**
	 * Returns the wrapped scanner
	 * @return IScanner
	 */
	public IScanner getScanner() {
		return fScanner;
	}
	
	/**
	 * Sets the scanner offset to the given offset.
	 */
	public void setOffset(int offset) {
		fScanner.resetTo(offset, fEndPosition);
	}
	
	/**
	 * Returns the offset after the current token
	 */	
	public int getCurrentEndOffset() {
		return fScanner.getCurrentTokenEndPosition() + 1;
	}

	/**
	 * Returns the start offset of the current token
	 */		
	public int getCurrentStartOffset() {
		return fScanner.getCurrentTokenStartPosition();
	}
	
	/**
	 * Returns the length of the current token
	 */	
	public int getCurrentLength() {
		return getCurrentEndOffset() - getCurrentStartOffset();
	}	

	/**
	 * Reads the next token.
	 * @param ignoreComments If set, comments will be overread
	 * @exception CoreException Thrown when the end of the file has been reached (code END_OF_FILE)
	 * or a lexical error was detected while scanning (code LEXICAL_ERROR)
	 */
	public int readNext(boolean ignoreComments) throws CoreException {
		int curr= 0;
		do {
			try {
				curr= fScanner.getNextToken();
				if (curr == ITerminalSymbols.TokenNameEOF) {
					throw new CoreException(JavaUIStatus.createError(END_OF_FILE, "End Of File", null)); //$NON-NLS-1$
				}
			} catch (InvalidInputException e) {
				throw new CoreException(JavaUIStatus.createError(LEXICAL_ERROR, e.getMessage(), e)); //$NON-NLS-1$
			}
		} while (ignoreComments && isComment(curr));
		return curr;
	}
	
	/**
	 * Reads the next token from the given offset.
	 * @param ignoreComments If set, comments will be overread
	 * @exception CoreException Thrown when the end of the file has been reached (code END_OF_FILE)
	 * or a lexical error was detected while scanning (code LEXICAL_ERROR)
	 */		
	public int readNext(int offset, boolean ignoreComments) throws CoreException {
		setOffset(offset);
		return readNext(ignoreComments);
	}
	
	/**
	 * Reads the next token from the given offset and returns the start offset of the token.
	 * @param ignoreComments If set, comments will be overread
	 * @exception CoreException Thrown when the end of the file has been reached (code END_OF_FILE)
	 * or a lexical error was detected while scanning (code LEXICAL_ERROR)
	 */	
	public int getNextStartOffset(int offset, boolean ignoreComments) throws CoreException {
		readNext(offset, ignoreComments);
		return getCurrentStartOffset();
	}
	
	/**
	 * Reads the next token from the given offset and returns the offset after the token.
	 * @param ignoreComments If set, comments will be overread
	 * @exception CoreException Thrown when the end of the file has been reached (code END_OF_FILE)
	 * or a lexical error was detected while scanning (code LEXICAL_ERROR)
	 */		
	public int getNextEndOffset(int offset, boolean ignoreComments) throws CoreException {
		readNext(offset, ignoreComments);
		return getCurrentEndOffset();
	}		

	/**
	 * Reads until a token is reached.
	 * @exception CoreException Thrown when the end of the file has been reached (code END_OF_FILE)
	 * or a lexical error was detected while scanning (code LEXICAL_ERROR)
	 */
	public void readToToken(int tok) throws CoreException {
		int curr= 0;
		do {
			curr= readNext(false);
		} while (curr != tok); 
	}

	/**
	 * Reads until a token is reached, starting from the given offset.
	 * @exception CoreException Thrown when the end of the file has been reached (code END_OF_FILE)
	 * or a lexical error was detected while scanning (code LEXICAL_ERROR)
	 */			
	public void readToToken(int tok, int offset) throws CoreException {
		setOffset(offset);
		readToToken(tok);
	}

	/**
	 * Reads from the given offset until a token is reached and returns the start offset of the token.
	 * @param token The token to be found.
	 * @exception CoreException Thrown when the end of the file has been reached (code END_OF_FILE)
	 * or a lexical error was detected while scanning (code LEXICAL_ERROR)
	 */	
	public int getTokenStartOffset(int token, int startOffset) throws CoreException {
		readToToken(token, startOffset);
		return getCurrentStartOffset();
	}	

	/**
	 * Reads from the given offset until a token is reached and returns the offset after the token.
	 * @param token The token to be found.
	 * @exception CoreException Thrown when the end of the file has been reached (code END_OF_FILE)
	 * or a lexical error was detected while scanning (code LEXICAL_ERROR)
	 */		
	public int getTokenEndOffset(int token, int startOffset) throws CoreException {
		readToToken(token, startOffset);
		return getCurrentEndOffset();
	}
	
	/**
	 * Reads from the given offset until a non-comment token is reached and returns the start offset of the comments
	 * directly ahead of the token.
	 * @param startOffset The offset to before the comments of the non-comment token.
	 * @param buffer The text buffer that corresponds to the content that is scanned or <code>null</code> if
	 * the line information shoould be taken from the scanner object
	 * @exception CoreException Thrown when the end of the file has been reached (code END_OF_FILE)
	 * or a lexical error was detected while scanning (code LEXICAL_ERROR)
	 */		
	public int getTokenCommentStart(int startOffset, TextBuffer buffer) throws CoreException {
		int curr= 0;
		int res= -1;
		int lastCommentEndLine= -1;
		setOffset(startOffset);
		do {
			curr= readNext(false);
			
			int startLine= getLineOfOffset(buffer, getCurrentStartOffset());
			if (lastCommentEndLine == -1 || startLine - lastCommentEndLine > 1) {
				res= getCurrentStartOffset();
			}
			lastCommentEndLine= getLineOfOffset(buffer, getCurrentEndOffset() - 1);
		} while (isComment(curr));
		return res;
	}
	
	private int getLineOfOffset(TextBuffer buffer, int offset) {
		if (buffer != null) {
			return buffer.getLineOfOffset(offset);
		}
		return fScanner.getLineNumber(offset);
	}
	
		
	public static boolean isComment(int token) {
		return token == ITerminalSymbols.TokenNameCOMMENT_BLOCK || token == ITerminalSymbols.TokenNameCOMMENT_JAVADOC 
			|| token == ITerminalSymbols.TokenNameCOMMENT_LINE;
	}
	
	public static boolean isModifier(int token) {
		switch (token) {
			case ITerminalSymbols.TokenNamepublic:
			case ITerminalSymbols.TokenNameprotected:
			case ITerminalSymbols.TokenNameprivate:
			case ITerminalSymbols.TokenNamestatic:
			case ITerminalSymbols.TokenNamefinal:
			case ITerminalSymbols.TokenNameabstract:
			case ITerminalSymbols.TokenNamenative:
			case ITerminalSymbols.TokenNamevolatile:
			case ITerminalSymbols.TokenNamestrictfp:
			case ITerminalSymbols.TokenNametransient:
			case ITerminalSymbols.TokenNamesynchronized:
				return true;
			default:
				return false;
		}
	}

}
