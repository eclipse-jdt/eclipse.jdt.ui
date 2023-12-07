/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core.manipulation.internal.javadoc;

import org.eclipse.text.readers.SingleCharacterReader;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.formatter.IndentManipulation;


/**
 * Reads a java doc comment from a java doc comment. Skips star-character on begin of line.
 */
public class CoreJavaDocCommentReader extends SingleCharacterReader {

	private IBuffer fBuffer;

	private String fSource;

	private int fCurrPos;

	private int fStartPos;

	private int fEndPos;

	private boolean fWasNewLine;

	public CoreJavaDocCommentReader(IBuffer buf, int start, int end) {
		fBuffer= buf;
		fStartPos= start + 3;
		fEndPos= end - 2;

		reset();
	}

	public CoreJavaDocCommentReader(String source, int start, int end) {
		fSource= source;
		fStartPos= start + 3;
		fEndPos= end - 2;

		reset();
	}

	/**
	 * @see java.io.Reader#read()
	 */
	@Override
	public int read() {
		if (fCurrPos < fEndPos) {
			char ch= getChar(fCurrPos++);
			if (fWasNewLine && !IndentManipulation.isLineDelimiterChar(ch)) {
				while (fCurrPos < fEndPos && Character.isWhitespace(ch)) {
					ch= getChar(fCurrPos++);
				}
				if (ch == '*') {
					if (fCurrPos < fEndPos) {
						do {
							ch= getChar(fCurrPos++);
						} while (ch == '*');
					} else {
						return -1;
					}
				}
			}
			fWasNewLine= IndentManipulation.isLineDelimiterChar(ch);

			return ch;
		}
		return -1;
	}

	/**
	 * @see java.io.Reader#close()
	 */
	@Override
	public void close() {
		fSource= null;
		fBuffer= null;
	}

	/**
	 * @see java.io.Reader#reset()
	 */
	@Override
	public void reset() {
		fCurrPos= fStartPos;
		fWasNewLine= true;
		// skip first line delimiter:
		if (fCurrPos < fEndPos && '\r' == getChar(fCurrPos)) {
			fCurrPos++;
		}
		if (fCurrPos < fEndPos && '\n' == getChar(fCurrPos)) {
			fCurrPos++;
		}
	}

	private char getChar(int pos) {
		if (fBuffer != null) {
			return fBuffer.getChar(pos);
		}
		return fSource.charAt(pos);
	}

	/**
	 * Returns the offset of the last read character in the passed buffer.
	 *
	 * @return the offset
	 */
	public int getOffset() {
		return fCurrPos;
	}

}
