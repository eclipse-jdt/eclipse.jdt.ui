package org.eclipse.jdt.internal.ui.text.javadoc;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jdt.core.IBuffer;

import org.eclipse.jdt.internal.ui.text.SingleCharReader;

/**
 * Reads a java doc comment from a java doc comment. Skips star-character
 * on begin of line
 */
public class JavaDocCommentReader extends SingleCharReader {

	private IBuffer fBuffer;
	
	private int fCurrPos;
	private int fStartPos;
	private int fEndPos;
	
	private boolean fWasNewLine;
		
	public JavaDocCommentReader(IBuffer buf, int start, int end) {
		fBuffer= buf;
		fStartPos= start + 3;
		fEndPos= end - 2;
		
		reset();
	}
	
	private final static boolean isNewLineCharacter(char ch) {
		return (ch == '\n' || ch == '\r');
	}		
		
	/**
	 * @see Reader#read(char)
	 */
	public int read() {
		if (fCurrPos < fEndPos) {
			char ch;
			if (fWasNewLine) {
				do {
					ch= fBuffer.getChar(fCurrPos++);
				} while (fCurrPos < fEndPos && Character.isWhitespace(ch));
				if (ch == '*') {
					if (fCurrPos < fEndPos) {
						do {
							ch= fBuffer.getChar(fCurrPos++);
						} while (ch == '*');
					} else {
						return -1;
					}
				}
			} else {
				ch= fBuffer.getChar(fCurrPos++);
			}
			fWasNewLine= isNewLineCharacter(ch);
			
			return ch;
		}
		return -1;
	}
		
	/**
	 * @see Reader#close()
	 */		
	public void close() {
		fBuffer= null;
	}
	
	/**
	 * @see Reader#reset()
	 */		
	public void reset() {
		fCurrPos= fStartPos;
		fWasNewLine= true;
	}	
		
		
}