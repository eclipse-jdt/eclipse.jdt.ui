/*
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 *
 * Contributors:
 *     IBM Corporation - initial implementation
 */
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
	
	public void mapElement(Object element, Widget item);
	
	public void unmapElement(Object element, Widget item);
	
	public Widget doFindInputItem(Object element);
	
	public Widget doFindItem(Object element);
	
	public void doUpdateItem(Widget item, Object element, boolean fullMap);
	
	public List getSelectionFromWidget();
	
	public void internalRefresh(Object element);
	
	public void setSelectionToWidget(List l, boolean reveal);
}
