/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.javaeditor;

import org.eclipse.swt.graphics.RGB;

import org.eclipse.jface.resource.ColorRegistry;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.ui.JavaUI;


/**
 * Semantic highlighting
 */
public abstract class SemanticHighlighting extends SemanticHighlightingCore {

	/**
	 * @return the preference key, will be augmented by a prefix and a suffix for each preference
	 */
	public abstract String getPreferenceKey();

	/**
	 * @return the default default text color
	 * @since 3.3
	 */
	public abstract RGB getDefaultDefaultTextColor();

	/**
	 * @return the default default text color
	 */
	public RGB getDefaultTextColor() {
		return findRGB(getThemeColorKey(), getDefaultDefaultTextColor());
	}

	/**
	 * @return <code>true</code> if the text attribute bold is set by default
	 */
	public abstract boolean isBoldByDefault();

	/**
	 * @return <code>true</code> if the text attribute italic is set by default
	 */
	public abstract boolean isItalicByDefault();

	/**
	 * @return <code>true</code> if the text attribute strikethrough is set by default
	 * @since 3.1
	 */
	public boolean isStrikethroughByDefault() {
		return false;
	}

	/**
	 * @return <code>true</code> if the text attribute underline is set by default
	 * @since 3.1
	 */
	public boolean isUnderlineByDefault() {
		return false;
	}

	/**
	 * @return <code>true</code> if the text attribute italic is enabled by default
	 */
	public abstract boolean isEnabledByDefault();

	private String getThemeColorKey() {
		return JavaUI.ID_PLUGIN + "." + getPreferenceKey() + "Highlighting";  //$NON-NLS-1$//$NON-NLS-2$
	}

	/**
	 * Returns the RGB for the given key in the given color registry.
	 *
	 * @param key the key for the constant in the registry
	 * @param defaultRGB the default RGB if no entry is found
	 * @return RGB the RGB
	 * @since 3.3
	 */
	private static RGB findRGB(String key, RGB defaultRGB) {
		if (!PlatformUI.isWorkbenchRunning())
			return defaultRGB;

		ColorRegistry registry= PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry();
		RGB rgb= registry.getRGB(key);
		if (rgb != null)
			return rgb;
		return defaultRGB;
	}

}
