/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.util;

import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.dialogs.Dialog;


/**
 * DO NOT REMOVE, used in a product.
 * @deprecated As of 3.5, replaced by {@link org.eclipse.jface.layout.PixelConverter}
 */
@Deprecated
public class PixelConverter {

	private final FontMetrics fFontMetrics;

	@Deprecated
	public PixelConverter(Control control) {
		this(control.getFont());
	}

	@Deprecated
	public PixelConverter(Font font) {
		GC gc = new GC(font.getDevice());
		gc.setFont(font);
		fFontMetrics= gc.getFontMetrics();
		gc.dispose();
	}

	/*
	 * see org.eclipse.jface.dialogs.DialogPage#convertHeightInCharsToPixels(int)
	 */
	@Deprecated
	public int convertHeightInCharsToPixels(int chars) {
		return Dialog.convertHeightInCharsToPixels(fFontMetrics, chars);
	}

	/*
	 * see org.eclipse.jface.dialogs.DialogPage#convertHorizontalDLUsToPixels(int)
	 */
	@Deprecated
	public int convertHorizontalDLUsToPixels(int dlus) {
		return Dialog.convertHorizontalDLUsToPixels(fFontMetrics, dlus);
	}

	/*
	 * see org.eclipse.jface.dialogs.DialogPage#convertVerticalDLUsToPixels(int)
	 */
	@Deprecated
	public int convertVerticalDLUsToPixels(int dlus) {
		return Dialog.convertVerticalDLUsToPixels(fFontMetrics, dlus);
	}

	/*
	 * see org.eclipse.jface.dialogs.DialogPage#convertWidthInCharsToPixels(int)
	 */
	@Deprecated
	public int convertWidthInCharsToPixels(int chars) {
		return Dialog.convertWidthInCharsToPixels(fFontMetrics, chars);
	}

}
