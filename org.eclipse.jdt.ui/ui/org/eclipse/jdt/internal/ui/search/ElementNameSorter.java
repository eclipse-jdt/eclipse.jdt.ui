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
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.search.ui.ISearchResultViewEntry;

/**
 * Sorts the search result viewer by the Java Element name.
 */
public class ElementNameSorter extends JavaSearchSorter {
	
	/*
	 * Overrides method from ViewerSorter
	 */
	public int compare(Viewer viewer, Object e1, Object e2) {
		String name1= null;
		String name2= null;

		if (e1 instanceof ISearchResultViewEntry)
			name1= getLabel(e1);
		if (e2 instanceof ISearchResultViewEntry)
			name2= getLabel(e2);
		if (name1 == null)
			name1= ""; //$NON-NLS-1$
		if (name2 == null)
			name2= ""; //$NON-NLS-1$
		return getCollator().compare(name1, name2);
	}

	protected int getSortOrder() {
		return JavaSearchResultLabelProvider.SHOW_ELEMENT_CONTAINER;
	}
}
