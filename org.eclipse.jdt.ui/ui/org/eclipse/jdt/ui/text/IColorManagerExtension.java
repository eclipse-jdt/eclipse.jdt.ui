/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
package org.eclipse.jdt.ui.text;

import org.eclipse.swt.graphics.RGB;


/**
 * Extends {@link org.eclipse.jdt.ui.text.IColorManager} with
 * the ability to bind and un-bind colors.
 *
 * @since 2.0
 */
public interface IColorManagerExtension {

	/**
	 * Remembers the given color specification under the given key.
	 *
	 * @param key the color key
	 * @param rgb the color specification
	 * @throws java.lang.UnsupportedOperationException if there is already a
	 * 	color specification remembered under the given key
	 */
	void bindColor(String key, RGB rgb);


	/**
	 * Forgets the color specification remembered under the given key.
	 *
	 * @param key the color key
	 */
	void unbindColor(String key);
}
