/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.javadoc;

import org.eclipse.jface.internal.text.html.SingleCharReader;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.formatter.IndentManipulation;


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

	/**
	 * @see java.io.Reader#read()
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
			fWasNewLine= IndentManipulation.isLineDelimiterChar(ch);

			return ch;
		}
		return -1;
	}

	/**
	 * @see java.io.Reader#close()
	 */
	public void close() {
		fBuffer= null;
	}

	/**
	 * @see java.io.Reader#reset()
	 */
	public void reset() {
		fCurrPos= fStartPos;
		fWasNewLine= true;
	}


	/**
	 * Returns the offset of the last read character in the passed buffer.
	 */
	public int getOffset() {
		return fCurrPos;
	}


}
