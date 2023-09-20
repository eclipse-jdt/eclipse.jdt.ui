/*******************************************************************************
 * Copyright (c) 2023 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java.hover;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;

public class JavaConstantHover extends AbstractJavaEditorTextHover {

	@Deprecated
	@Override
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		if (hoverRegion.getLength() > 0) {
			String hoverSource= ""; //$NON-NLS-1$
			IDocument document= textViewer.getDocument();
			int offset= hoverRegion.getOffset();
			int length= hoverRegion.getLength();
			boolean negated= false;
			try {
				if (offset > 1) {
					char prevChar= document.getChar(offset - 1);
					if (prevChar == '.') {
						--offset;
						++length;
						while (offset > 1 && document.get(offset - 1, 1).toLowerCase().charAt(0) != 'x'
								&& Character.digit(document.getChar(offset - 1), 16) != -1) {
							--offset;
							++length;
						}
						if (offset > 1 && document.get(offset - 1, 1).toLowerCase().charAt(0) == 'x') {
							--offset;
							++length;
							if (document.getChar(offset - 1) == '0') {
								--offset;
								++length;
							}
							if (offset > 0) {
								prevChar= document.getChar(offset - 1);
							}
						}

					}
					if (prevChar == '-' || prevChar == '+') {
						if (offset > 4 && document.get(offset - 2, 1).toLowerCase().charAt(0) == 'p') {
							char prevPrevChar= document.getChar(offset - 3);
							if (prevPrevChar == '.' || Character.digit(prevPrevChar, 16) != -1) {
								prevChar= ' ';
								offset -= 2;
							    length += 2;
							    while (offset > 0 && (document.getChar(offset - 1) == '.' || Character.digit(document.getChar(offset - 1), 16) != -1)) {
									--offset;
									++length;
								}
								if (offset > 2 && document.get(offset - 2, 2).toLowerCase().equals("0x")) { //$NON-NLS-1$
									offset -= 2;
									length += 2;
								}
								if (offset > 1) {
									prevChar= document.getChar(offset - 1);
								}
							}
						}
						char prevPrevChar= ' ';
						if (offset > 1) {
							prevPrevChar= document.getChar(offset - 2);
						}
						negated= prevChar == '-' && !Character.isLetterOrDigit(prevPrevChar);
					}
					if (offset + length < document.getLength() &&
							document.getChar(offset + length) == '.') {
						++length;
						while (offset + length <= document.getLength()
								&& Character.digit(document.getChar(offset + length), 16) != -1
								|| document.get(offset + length, 1).toLowerCase().charAt(0) == 'p') {
							++length;
						}
					}
					if (offset + length <= document.getLength() + 2
							&& document.get(offset + length - 1, 1).toLowerCase().charAt(0) == 'p') {
						++length;
						char nextChar= document.getChar(offset + length);
						if (nextChar == '-' || nextChar == '+') {
							++length;
						}
						while (offset + length <= document.getLength()
								&& Character.digit(document.getChar(offset + length), 16) != -1) {
							++length;
						}
					}
				}
				hoverSource= document.get(offset, length);

			} catch (BadLocationException e) {
				// should never happen
			}
			hoverSource= hoverSource.toLowerCase();
			if (hoverSource != null && hoverSource.startsWith("0")) { //$NON-NLS-1$
				try {
					if (hoverSource.charAt(hoverSource.length() - 1) == 'l') {
						String temp= hoverSource.substring(0, hoverSource.length() - 1).toLowerCase();
						long longValue= temp.startsWith("0b") ? //$NON-NLS-1$
								Long.parseUnsignedLong(withoutUnderscoreInfixes(temp.substring(2)), 2)
								: (temp.startsWith("0x") //$NON-NLS-1$
										? Long.parseUnsignedLong(withoutUnderscoreInfixes(temp.substring(2)), 16)
												: Long.decode(withoutUnderscoreInfixes(temp)).longValue());
						if (negated) {
							longValue= -longValue;
						}
						return "<body><p>" + Long.toString(longValue) + "<b> : [0x" + Long.toHexString(longValue) + "]</p></body>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					} else {
						int intValue= hoverSource.startsWith("0b") ? //$NON-NLS-1$
								Integer.parseUnsignedInt(withoutUnderscoreInfixes(hoverSource.substring(2)), 2)
								: (hoverSource.startsWith("0x") //$NON-NLS-1$
										? Integer.parseUnsignedInt(withoutUnderscoreInfixes(hoverSource.substring(2)), 16)
												: Integer.decode(withoutUnderscoreInfixes(hoverSource)).intValue());
						if (negated) {
							intValue= -intValue;
						}
						return "<body><p>" + Integer.toString(intValue) + "<b> : [0x" + Integer.toHexString(intValue) + "]</p></body>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

					}
				} catch (NumberFormatException e) {
					try {
						if (hoverSource.startsWith("0x")) { //$NON-NLS-1$
							if (hoverSource.endsWith("p")) { //$NON-NLS-1$
								++length;
								while (offset + length <= document.getLength() && !Character.isWhitespace(document.get(offset + length + 1, 1).charAt(0))) {
									++length;
								}
								hoverSource= document.get(offset, length).toLowerCase();
							}
							double doubleValue= Double.valueOf(withoutUnderscoreInfixes(hoverSource)).doubleValue();
							if (negated) {
								doubleValue= -doubleValue;
							}
							return "<body><p>" + Double.toString(doubleValue) + "</p></body>"; //$NON-NLS-1$ //$NON-NLS-2$
						}
					} catch (NumberFormatException | BadLocationException e1) {
						// do nothing
					}
				}
			}
		}
		return null;
	}

	private static String withoutUnderscoreInfixes(String s) {
		if ((s.length() > 0) && (s.indexOf('_') <= 0 || s.lastIndexOf('_') == s.length() - 1) || s.startsWith("0x_") || s.startsWith("0X_")) { //$NON-NLS-1$ //$NON-NLS-2$
			return s;
		}
		return s.replace("_", ""); //$NON-NLS-1$ //$NON-NLS-2$
	}

}
