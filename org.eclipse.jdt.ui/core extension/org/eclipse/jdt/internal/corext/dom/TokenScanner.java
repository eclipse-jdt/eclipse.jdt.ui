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

import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;

import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;

/**
 * Wraps a scanner and offers convenient methods for finding tokens 
 */
public class TokenScanner {
	
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
	public int getCurrentEndOffset() throws InvalidInputException {
		return fScanner.getCurrentTokenEndPosition() + 1;
	}

	/**
	 * Returns the start offset of the current token
	 */		
	public int getCurrentStartOffset() throws InvalidInputException {
		return fScanner.getCurrentTokenStartPosition();
	}
	
	/**
	 * Returns the length of the current token
	 */	
	public int getCurrentLength() throws InvalidInputException {
		return getCurrentEndOffset() - getCurrentStartOffset();
	}	

	/**
	 * Reads the next token.
	 * @param ignoreComments If set, comments will be overread
	 * @exception InvalidInputException Thrown when the end of the file has been reached 
	 */
	public int readNext(boolean ignoreComments) throws InvalidInputException {
		int curr= 0;
		do {
			curr= fScanner.getNextToken();
			if (curr == ITerminalSymbols.TokenNameEOF) {
				throw new InvalidInputException("End of File"); //$NON-NLS-1$
			}
		} while (ignoreComments && isComment(curr));
		return curr;
	}
	
	/**
	 * Reads the next token from the given offset.
	 * @param ignoreComments If set, comments will be overread
	 * @exception InvalidInputException Thrown when the end of the file has been reached 
	 */		
	public int readNext(int offset, boolean ignoreComments) throws InvalidInputException {
		setOffset(offset);
		return readNext(ignoreComments);
	}
	
	/**
	 * Reads the next token from the given offset and returns the start offset of the token.
	 * @param ignoreComments If set, comments will be overread
	 * @exception InvalidInputException Thrown when the end of the file has been reached 
	 */	
	public int getNextStartOffset(int offset, boolean ignoreComments) throws InvalidInputException {
		readNext(offset, ignoreComments);
		return getCurrentStartOffset();
	}
	
	/**
	 * Reads the next token from the given offset and returns the offset after the token.
	 * @param ignoreComments If set, comments will be overread
	 * @exception InvalidInputException Thrown when the end of the file has been reached 
	 */		
	public int getNextEndOffset(int offset, boolean ignoreComments) throws InvalidInputException {
		readNext(offset, ignoreComments);
		return getCurrentEndOffset();
	}		

	/**
	 * Reads until a token is reached.
	 * @exception InvalidInputException Thrown when the end of the file has been reached 
	 */
	public void readToToken(int tok) throws InvalidInputException {
		int curr= 0;
		do {
			curr= fScanner.getNextToken();
			if (curr == ITerminalSymbols.TokenNameEOF) {
				throw new InvalidInputException("End of File"); //$NON-NLS-1$
			}
		} while (curr != tok); 
	}

	/**
	 * Reads until a token is reached, starting from the given offset.
	 * @exception InvalidInputException Thrown when the end of the file has been reached 
	 */			
	public void readToToken(int tok, int offset) throws InvalidInputException {
		setOffset(offset);
		readToToken(tok);
	}

	/**
	 * Reads from the given offset until a token is reached and returns the start offset of the token.
	 * @param token The token to be found.
	 * @exception InvalidInputException Thrown when the end of the file has been reached 
	 */	
	public int getTokenStartOffset(int token, int startOffset) throws InvalidInputException {
		readToToken(token, startOffset);
		return getCurrentStartOffset();
	}	

	/**
	 * Reads from the given offset until a token is reached and returns the offset after the token.
	 * @param token The token to be found.
	 * @exception InvalidInputException Thrown when the end of the file has been reached 
	 */		
	public int getTokenEndOffset(int token, int startOffset) throws InvalidInputException {
		readToToken(token, startOffset);
		return getCurrentEndOffset();
	}
	
	/**
	 * Reads from the given offset until a non-comment token is reached and returns the start offset of the comments
	 * directly ahead of the token.
	 * @exception InvalidInputException Thrown when the end of the file has been reached 
	 */		
	public int getTokenCommentStart(int startOffset, TextBuffer buffer) throws InvalidInputException {
		int curr= 0;
		int res= -1;
		int lastCommentEndLine= -1;
		setOffset(startOffset);
		do {
			curr= fScanner.getNextToken();
			if (curr == ITerminalSymbols.TokenNameEOF) {
				throw new InvalidInputException("End of File"); //$NON-NLS-1$
			}
			
			int startLine= buffer.getLineOfOffset(getCurrentStartOffset());
			if (lastCommentEndLine == -1 || startLine - lastCommentEndLine > 1) {
				res= getCurrentStartOffset();
			}
			lastCommentEndLine= buffer.getLineOfOffset(getCurrentEndOffset() - 1);
		} while (isComment(curr));
		return res;
	}	
	

	public int readAfterTokens(int[] tokens, boolean ignoreComments, int startOffset, int defaultPos) throws InvalidInputException {
		setOffset(startOffset);
		int pos= defaultPos;
		loop: while(true) {
			int curr= readNext(ignoreComments);
			for (int i= 0; i < tokens.length; i++) {
				if (tokens[i] == curr) {
					pos= getCurrentEndOffset();
					continue loop;
				}
			}
			return pos;
		}
	}	
	
	public static boolean isComment(int token) {
		return token == ITerminalSymbols.TokenNameCOMMENT_BLOCK || token == ITerminalSymbols.TokenNameCOMMENT_JAVADOC 
			|| token == ITerminalSymbols.TokenNameCOMMENT_LINE;
	}	


}
