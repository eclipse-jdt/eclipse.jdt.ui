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
package org.eclipse.jdt.internal.ui.browsing;

import java.util.List;

import org.eclipse.swt.widgets.Widget;

/**
 * Allows accessing the PackagesViewTableViewer and the
 * PackagesViewTreeViewer with identical API.
 *
 * @since 2.1
 */
interface IPackagesViewViewer {

	void mapElement(Object element, Widget item);

	void unmapElement(Object element, Widget item);

	Widget doFindInputItem(Object element);

	Widget doFindItem(Object element);

	void doUpdateItem(Widget item, Object element, boolean fullMap);

	@SuppressWarnings("rawtypes")
	List getSelectionFromWidget();

	void internalRefresh(Object element);

	@SuppressWarnings("rawtypes")
	void setSelectionToWidget(List l, boolean reveal);
}
