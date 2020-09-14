/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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


import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jdt.ui.text.IColorManager;
import org.eclipse.jdt.ui.text.IColorManagerExtension;

/**
 * Java color manager.
 */
public class JavaColorManager implements IColorManager, IColorManagerExtension {

	protected Map<String, RGB> fKeyTable= new HashMap<>(10);
	protected Map<Display, Map<RGB, Color>> fDisplayTable= new HashMap<>(2);

	/*
	 * @see IColorManager#getColor(RGB)
	 */
	@Override
	public Color getColor(RGB rgb) {

		if (rgb == null)
			return null;

		final Display display= Display.getCurrent();
		Map<RGB, Color> colorTable= fDisplayTable.get(display);
		if (colorTable == null) {
			colorTable= new HashMap<>(10);
			fDisplayTable.put(display, colorTable);
		}

		Color color= colorTable.get(rgb);
		if (color == null) {
			color= new Color(Display.getCurrent(), rgb);
			colorTable.put(rgb, color);
		}

		return color;
	}

	/*
	 * @see IColorManager#dispose
	 */
	@Override
	public void dispose() {
		// no-op
	}


	/*
	 * @see IColorManager#getColor(String)
	 */
	@Override
	public Color getColor(String key) {

		if (key == null)
			return null;

		RGB rgb= fKeyTable.get(key);
		return getColor(rgb);
	}

	/*
	 * @see IColorManagerExtension#bindColor(String, RGB)
	 */
	@Override
	public void bindColor(String key, RGB rgb) {
		Object value= fKeyTable.get(key);
		if (value != null)
			throw new UnsupportedOperationException();

		fKeyTable.put(key, rgb);
	}

	/*
	 * @see IColorManagerExtension#unbindColor(String)
	 */
	@Override
	public void unbindColor(String key) {
		fKeyTable.remove(key);
	}
}
