/*
 * (c) Copyright 2002 IBM Corporation.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.dom;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.compiler.parser.InvalidInputException;
import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.compiler.parser.TerminalSymbols;

public class CompilationUnitBuffer {
	private Scanner fScanner;
	private int fEndPosition;
	
	public CompilationUnitBuffer(ICompilationUnit unit) throws JavaModelException {
		fScanner= new Scanner(false, false);	// comments and white spaces.
		char[] source= unit.getBuffer().getCharacters();
		fEndPosition= source.length - 1;
		fScanner.setSourceBuffer(source);
	}
	
	public char[] getCharacters() {
		return fScanner.source;
	}
	
	public char getCharAt(int index) {
		return fScanner.source[index];
	}
	
	public int indexOf(int searchFor, int start) {
		return indexOf(searchFor, start, fEndPosition);
	}
	
	public int indexOf(int searchFor, int start, int length) {
		if (length <= 0)
			return -1;
		try {
			fScanner.resetTo(start, start + length - 1);
			int token;
			while((token= fScanner.getNextToken()) != TerminalSymbols.TokenNameEOF) {
				if (token == searchFor) {
					return fScanner.startPosition;
				}
			} 
			return -1;
		} catch (InvalidInputException e) {
			return -1;
		}
	}

	public int indexAfter(int searchFor, int start) {
		int result= indexOf(searchFor, start);
		if (result == -1)
			return result;
		return fScanner.currentPosition;
	}
	
	/**
	 * Scans for the next statement character. White spaces, comments and semicolons
	 * (';') are not interpreted to be a statment character.
	 * 
	 * @return the index of the next statement character or -1 if no appropriate
	 *  character was found.
	 */
	public int indexOfStatementCharacter(int start) {
		try {
			fScanner.resetTo(start, fEndPosition);
			int token;
			while ((token= fScanner.getNextToken()) != TerminalSymbols.TokenNameEOF) {
				if (token == Scanner.TokenNameSEMICOLON)
					continue;
				return fScanner.startPosition;
			}
			return -1;
		} catch (InvalidInputException e) {
			return -1;
		}
	}
	
	public int getLastTokenLength() {
		return fScanner.currentPosition - fScanner.startPosition;
	}
}
