/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.text;


import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

public class JavaWordFinder {

	public static IRegion findWord(IDocument document, int offset) {

		int start= -2;
		int end= -1;

		try {
			int pos= offset;
			char c;

			while (pos >= 0) {
				c= document.getChar(pos);
				if (!Character.isJavaIdentifierPart(c)) {
					if (Character.isSurrogate(c)) {
						int codePoint;
						if (Character.isLowSurrogate(c) && pos > 0) {
							char c2= document.getChar(pos - 1);
							if (Character.isHighSurrogate(c2)) {
								codePoint= Character.toCodePoint(c2, c);
								if (Character.isJavaIdentifierPart(codePoint)) {
									pos--;
								} else {
									break;
								}
							} else {
								break;
							}
						} else {
							break;
						}
					} else {
						break;
					}
				}
				--pos;
			}
			start= pos;

			pos= offset;
			int length= document.getLength();

			while (pos < length) {
				c= document.getChar(pos);
				if (!Character.isJavaIdentifierPart(c)) {
					if (Character.isSurrogate(c)) {
						int codePoint;
						if (Character.isHighSurrogate(c) && pos + 1 < length) {
							char c2= document.getChar(pos + 1);
							if (Character.isLowSurrogate(c2)) {
								codePoint= Character.toCodePoint(c, c2);
								if (Character.isJavaIdentifierPart(codePoint)) {
									pos++;
								} else {
									break;
								}
							} else {
								break;
							}
						} else {
							break;
						}
					} else {
						break;
					}

				}
				++pos;
			}
			end= pos;

		} catch (BadLocationException x) {
		}

		if (start >= -1 && end > -1) {
			if (start == offset && end == offset) {
				try {
					char c= document.getChar(offset);
					switch (c) {
						case '-':
							if (document.getChar(offset + 1) == '>') {
								return new Region(offset, 2);
							}
							break;
						case '>':
							if (document.getChar(offset - 1) == '-') {
								return new Region(offset - 1, 2);
							}
							break;
						case ':':
							if (document.getChar(offset + 1) == ':') {
								return new Region(offset, 2);
							} else if (document.getChar(offset - 1) == ':') {
								return new Region(offset - 1, 2);
							}
							break;
					}
				} catch (BadLocationException e) {
				}
				return new Region(offset, 0);
			} else if (start == offset) {
				return new Region(start, end - start); //XXX: probably unused...
			} else {
				return new Region(start + 1, end - start - 1);
			}
		}

		return null;
	}

	private JavaWordFinder() {
	}
}
