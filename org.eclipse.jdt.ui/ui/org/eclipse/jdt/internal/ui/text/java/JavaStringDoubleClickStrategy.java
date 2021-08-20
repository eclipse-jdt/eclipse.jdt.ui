/*******************************************************************************
 * Copyright (c) 2021 Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Thomas Wolf - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.text.java;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextUtilities;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.text.ISourceVersionDependent;

/**
 * A double click strategy that handles Java character escapes as "words".
 */
public class JavaStringDoubleClickStrategy extends PartitionDoubleClickSelector implements ISourceVersionDependent {

	private boolean hasSpaceEscape = false;

	public JavaStringDoubleClickStrategy(String partitioning, int leftBorder, int rightBorder) {
		super(partitioning, leftBorder, rightBorder);
	}

	public JavaStringDoubleClickStrategy(String partitioning, int leftBorder, int rightBorder, int hitDeltaOffset) {
		super(partitioning, leftBorder, rightBorder, hitDeltaOffset);
	}

	protected IRegion getPartitionRegion(IDocument document, int offset) throws BadLocationException {
		return TextUtilities.getPartition(document, fPartitioning, offset, true);
	}

	@Override
	protected IRegion findWord(IDocument document, int offset) {
		IRegion word= super.findWord(document, offset);
		try {
			IRegion line= document.getLineInformationOfOffset(offset);
			IRegion partition= getPartitionRegion(document, offset);
			// Restrict the line to the partition
			int regionStart= Math.max(line.getOffset(), partition.getOffset() + fLeftBorder);
			int regionEnd= Math.min(line.getOffset() + line.getLength(), partition.getOffset() + partition.getLength() - fRightBorder);
			if (regionEnd <= regionStart) {
				return word;
			}
			line= new Region(regionStart, regionEnd - regionStart);
			if (offset <= line.getOffset() || offset > regionEnd) {
				return word;
			}
			String text= document.get(line.getOffset(), line.getLength());
			int textOffset;
			int posInLine= offset - line.getOffset();
			if (word == null) {
				textOffset= posInLine;
			} else {
				textOffset= word.getOffset() - line.getOffset();
			}
			if (textOffset <= 0) {
				return word;
			}
			int escapeStart= findEscapeStart(text, textOffset);
			if (escapeStart < 0) {
				return word;
			}
			int escapeEnd= findEscapeEnd(text, escapeStart + 1);
			if (escapeEnd < 0) {
				// Wasn't a valid escape after all
				return word;
			}
			if (escapeEnd > textOffset) {
				if (posInLine <= escapeEnd) {
					// The escape is the word
					return new Region(escapeStart + line.getOffset(), escapeEnd - escapeStart);
				}
				// The word is to be shortened
				if (word == null) {
					return null;
				}
				int start= escapeEnd + line.getOffset();
				int length= word.getLength() - (escapeEnd - textOffset);
				if (length <= 0) {
					return null;
				}
				return new Region(start, length);
			}
			return word;
		} catch (BadLocationException x) {
			return word;
		}
	}

	private int findEscapeStart(String text, int from) {
		int i= Math.min(from, text.length() - 1);
		int limit= Math.max(0, from - 6); // Longest escape is \u0123: 6 characters
		char ch= ' ';
		for (; i >= limit; i--) {
			ch= text.charAt(i);
			if (ch == '\\' || Character.isWhitespace(ch)) {
				break;
			}
		}
		if (ch != '\\') {
			return -1;
		}
		// Count backslashes. It's only the start of an escape if there is an odd number of backslashes.
		int n= 1;
		for (int j= i - 1; j >= 0 && text.charAt(j) == '\\'; j--) {
			n++;
		}
		// If the number is even, the previous backslash is the start of the escape.
		return ((n & 1) == 0) ? i - 1 : i;
	}

	private int findEscapeEnd(String text, int from) {
		int length= text.length();
		if (from >= length) {
			return -1;
		}
		char ch= text.charAt(from);
		switch (ch) {
			case 'b': // backspace
			case 'f': // form feed
			case 'n': // newline
			case 'r': // carriage return
			case 't': // tab
			case '\\':
			case '\'':
			case '"':
				return from + 1;
			case 's':
				return hasSpaceEscape ? from + 1 : -1;
			case 'u':
				// Four hex digits
				if (length - from > 4) {
					for (int i= from + 4; i > from; i--) {
						if (!isHex(text.charAt(i))) {
							return -1;
						}
					}
					return from + 5;
				}
				return -1;
			default:
				if (ch >= '0' && ch <= '7') {
					// Octal escape
					int max= (ch <= '3') ? 3 : 2;
					int i= from + 1;
					int limit= Math.min(length, from + max);
					for (; i < limit; i++) {
						ch= text.charAt(i);
						if (!(ch >= '0' && ch <= '7')) {
							break;
						}
					}
					return i;
				}
				return -1;
		}
	}

	private boolean isHex(char ch) {
		return (ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F');
	}

	@Override
	public void setSourceVersion(String version) {
		hasSpaceEscape= JavaCore.compareJavaVersions(JavaCore.VERSION_15, version) <= 0;
	}
}
