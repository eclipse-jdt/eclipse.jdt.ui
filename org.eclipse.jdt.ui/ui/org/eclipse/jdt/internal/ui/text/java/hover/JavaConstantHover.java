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
			try {
				hoverSource= document.get(hoverRegion.getOffset(), hoverRegion.getLength());
			} catch (BadLocationException e) {
				// should never happen
			}
			if (hoverSource != null && hoverSource.startsWith("0")) { //$NON-NLS-1$
				try {
					long longValue= hoverSource.startsWith("0b") ? //$NON-NLS-1$
							Long.parseLong(hoverSource.substring(2), 2) : Long.decode(hoverSource).longValue();
					return "<body><p>" + Long.toString(longValue) + "<b> : [0x" + Long.toHexString(longValue) + "]</p></body>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				} catch (NumberFormatException e) {
					try {
						double doubleValue= Double.valueOf(hoverSource).doubleValue();
						return "<body><p>" + Double.toString(doubleValue) + "</p></body>"; //$NON-NLS-1$ //$NON-NLS-2$
					} catch (NumberFormatException e1) {
						// do nothing
					}
				}
			}
		}
		return null;
	}

}
