/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
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
 * Note:
 *     The code moved from org.eclipse.jdt.internal.debug.ui from the eclipse.jdt.debug project
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.filtertable;


import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;

/**
 * Label provider for Filter model objects
 * @since 3.26
 */
public class FilterLabelProvider extends LabelProvider implements ITableLabelProvider {

	private static final Image IMG_CUNIT =
		JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_CLASS);
	private static final Image IMG_PKG =
		JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_PACKAGE);

	/**
	 * @see ITableLabelProvider#getColumnText(Object, int)
	 */
	@Override
	public String getColumnText(Object object, int column) {
		if (column == 0) {
			return ((Filter) object).getName();
		}
		return ""; //$NON-NLS-1$
	}

	/**
	 * @see ILabelProvider#getText(Object)
	 */
	@Override
	public String getText(Object element) {
		return ((Filter) element).getName();
	}

	/**
	 * @see ITableLabelProvider#getColumnImage(Object, int)
	 */
	@Override
	public Image getColumnImage(Object object, int column) {
		String name = ((Filter) object).getName();
		if (name.endsWith("*") || name.equals("(default package)")) { //$NON-NLS-1$ //$NON-NLS-2$
			return IMG_PKG;
		}
		return IMG_CUNIT;
	}
}
