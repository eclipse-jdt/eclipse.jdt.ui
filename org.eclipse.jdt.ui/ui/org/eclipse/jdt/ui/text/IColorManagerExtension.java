/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.text;

import org.eclipse.swt.graphics.RGB;

/**
 * A color manager extension for extending <code>IColorManager</code> 
 * instances with new functionality.
 * 
 * @since 2.0
 */
public interface IColorManagerExtension {
	
	/**
	 * Remembers the given color specification under the given key.
	 *
	 * @param key the color key
	 * @param rgb the color specification
	 * @exception UnsupportedOperationException if there is already a
	 * 	color specification remembered under the given key
	 */
	void bindColor(String key, RGB rgb);
	
	
	/**
	 * Forgets the color specification remembered under the given key.
	 * @param key the color key
	 */
	void unbindColor(String key);
}
