/*
 * (c) Copyright 2002 IBM Corporation.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.dom;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;

public class CompilationUnitBuffer {
	private IScanner fScanner;
	private int fEndPosition;
	
	public CompilationUnitBuffer(ICompilationUnit unit) throws JavaModelException {
		fScanner= ToolFactory.createScanner(true, false, false, false);
		char[] source= unit.getBuffer().getCharacters();
		fEndPosition= source.length - 1;
		fScanner.setSource(source);
	}
	
	public char[] getCharacters() {
		return fScanner.getSource();
	}
	
	public char getCharAt(int index) {
		return fScanner.getSource()[index];
	}
	
	public int indexOf(int token, int start) {
		return indexOf(token, start, fEndPosition);
	}
	
	public int indexOf(int token, int start, int length) {
		if (length <= 0)
			return -1;
		try {
			fScanner.resetTo(start, start + length - 1);
			int next;
			while((next= fScanner.getNextToken()) != ITerminalSymbols.TokenNameEOF) {
				if (next == token) {
					return fScanner.getCurrentTokenStartPosition();
				}
			} 
			return -1;
		} catch (InvalidInputException e) {
			return -1;
		}
	}

	public int indexAfter(int token, int start) {
		int result= indexOf(token, start);
		if (result == -1)
			return result;
		return fScanner.getCurrentTokenEndPosition();
	}
	
	/**
	 * Returns the index of the next token. White spaces are ignored. Comments
	 * are ignored if <code>considerComments</code> is set to <code>false
	 * </code>.
	 * 
	 * @param considerComments <code>true</code> if comments are to be
	 * 	considered; otherwise <code>false</code>
	 * @return the index of the next token or -1 if no appropriate toke was found
	 */
	public int indexOfNextToken(int start, boolean considerComments) {
		try {
			fScanner.resetTo(start, fEndPosition);
			int token;
			while ((token= fScanner.getNextToken()) != ITerminalSymbols.TokenNameEOF) {
				if (!considerComments && isComment(token))
					continue;
				return fScanner.getCurrentTokenStartPosition();
			}
			return -1;
		} catch (InvalidInputException e) {
			return -1;
		}
	}

	private boolean isComment(int token) {
		return token == ITerminalSymbols.TokenNameCOMMENT_BLOCK || token == ITerminalSymbols.TokenNameCOMMENT_JAVADOC 
			|| token == ITerminalSymbols.TokenNameCOMMENT_LINE;
	}	
}
