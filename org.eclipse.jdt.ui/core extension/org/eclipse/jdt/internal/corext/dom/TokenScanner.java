/*******************************************************************************
 * Copyright (c) 2000, 2003 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Common Public License v0.5 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/cpl-v05.
 * html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.dom;

import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;

public class TokenScanner {

	private IScanner fScanner;
	private int fEndPosition;
	
	public TokenScanner(IScanner scanner) {
		fScanner= scanner;
		fEndPosition= fScanner.getSource().length - 1;
	}
		
	public IScanner getScanner() {
		return fScanner;
	}

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
	
	public void setOffset(int offset) {
		fScanner.resetTo(offset, fEndPosition);
	}
		
	public int readNext(int offset, boolean ignoreComments) throws InvalidInputException {
		setOffset(offset);
		return readNext(ignoreComments);
	}	
	
	public void readToToken(int tok, int offset) throws InvalidInputException {
		setOffset(offset);
		readToToken(tok);
	}
	
	public void readToToken(int tok) throws InvalidInputException {
		int curr= 0;
		do {
			curr= fScanner.getNextToken();
			if (curr == ITerminalSymbols.TokenNameEOF) {
				throw new InvalidInputException("End of File"); //$NON-NLS-1$
			}
		} while (curr != tok); 
	}	

	public int getNextStartOffset(int offset, boolean ignoreComments) throws InvalidInputException {
		readNext(offset, ignoreComments);
		return getCurrentStartOffset();
	}
	
	public int getNextEndOffset(int offset, boolean ignoreComments) throws InvalidInputException {
		readNext(offset, ignoreComments);
		return getCurrentEndOffset();
	}	
	
	public int getCurrentEndOffset() throws InvalidInputException {
		return fScanner.getCurrentTokenEndPosition() + 1;
	}
	
	public int getCurrentStartOffset() throws InvalidInputException {
		return fScanner.getCurrentTokenStartPosition();
	}
	
	public int getCurrentLength() throws InvalidInputException {
		return getCurrentEndOffset() - getCurrentStartOffset();
	}
	
	public int getTokenStartOffset(int token, int startOffset) throws InvalidInputException {
		readToToken(token, startOffset);
		return getCurrentStartOffset();
	}	
	
	public int getTokenEndOffset(int token, int startOffset) throws InvalidInputException {
		readToToken(token, startOffset);
		return getCurrentEndOffset();
	}
	
	public int getTokenEndOffset(int[] tokens, int startOffset, int defaultPos) throws InvalidInputException {
		int pos= defaultPos;
		loop: while(true) {
			int curr= readNext(pos, false);
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
