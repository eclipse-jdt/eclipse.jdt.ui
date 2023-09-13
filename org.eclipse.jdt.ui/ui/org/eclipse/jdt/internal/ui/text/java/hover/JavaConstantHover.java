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
		if (hoverRegion.getLength() > 1) {
			String hoverSource= null;
			IDocument document= textViewer.getDocument();
			int offset= hoverRegion.getOffset();
			int length= hoverRegion.getLength();
			try {
				if (offset > 1) {
					if (document.get(offset - 1, 1).equals(".")) { //$NON-NLS-1$
						--offset;
						++length;
						while (offset > 1 && !Character.isWhitespace(document.get(offset - 1, 1).charAt(0))) {
							--offset;
							++length;
						}
					}
					if (document.get(offset + length, 1).equals(".")) { //$NON-NLS-1$
						++length;
						while (offset + length <= document.getLength() && !Character.isWhitespace(document.get(offset + length + 1, 1).charAt(0))) {
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
					String temp= hoverSource.charAt(hoverSource.length() - 1) == 'l' ? hoverSource.substring(0, hoverSource.length() - 1) : hoverSource;
					long longValue= temp.startsWith("0b") ? //$NON-NLS-1$
							Long.parseLong(withoutUnderscoreInfixes(temp.substring(2)), 2)
							: Long.decode(withoutUnderscoreInfixes(temp)).longValue();
					return "<body><p>" + Long.toString(longValue) + "<b> : [0x" + Long.toHexString(longValue) + "]</p></body>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				} catch (NumberFormatException e) {
					try {
						if (hoverSource.startsWith("0x")) { //$NON-NLS-1$
							double doubleValue= Double.valueOf(hoverSource).doubleValue();
							return "<body><p>" + Double.toString(doubleValue) + "</p></body>"; //$NON-NLS-1$ //$NON-NLS-2$
						}
					} catch (NumberFormatException e1) {
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
