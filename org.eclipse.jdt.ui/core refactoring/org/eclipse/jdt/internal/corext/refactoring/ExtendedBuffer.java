/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;

import org.eclipse.jdt.internal.compiler.parser.Scanner;

public class ExtendedBuffer {
	private Scanner fScanner;
	private int fEndPosition;
	private int fLength;
	
	public ExtendedBuffer(IBuffer buffer) {
		fScanner= new Scanner(false, false);	// comments and white spaces.
		char[] source= buffer.getCharacters();
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
	
	public int indexOf(int searchFor, int start, int end) {
		try {
			fLength= -1;
			fScanner.resetTo(start, end);
			int token;
			while((token= fScanner.getNextToken()) != ITerminalSymbols.TokenNameEOF) {
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
			fLength= -1;
			fScanner.resetTo(start, fEndPosition);
			int token;
			while ((token= fScanner.getNextToken()) != ITerminalSymbols.TokenNameEOF) {
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