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

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

import org.eclipse.search.ui.ISearchResultView;
import org.eclipse.search.ui.ISearchResultViewEntry;
import org.eclipse.search.ui.SearchUI;

/**
 * Sorts the search result viewer by the Java Element parent name.
 */
public class ParentNameSorter extends ViewerSorter {

	private ILabelProvider fLabelProvider;
	
	/*
	 * Overrides method from ViewerSorter
	 */
	public int compare(Viewer viewer, Object e1, Object e2) {
		String name1= null;
		String name2= null;
		
		if (e1 instanceof ISearchResultViewEntry)
			name1= fLabelProvider.getText(e1);
		if (e2 instanceof ISearchResultViewEntry)
			name2= fLabelProvider.getText(e2);
		if (name1 == null)
			name1= ""; //$NON-NLS-1$
		if (name2 == null)
			name2= ""; //$NON-NLS-1$
		return getCollator().compare(name1, name2);
	}

	/*
	 * Overrides method from ViewerSorter
	 */
	public boolean isSorterProperty(Object element, String property) {
		return true;
	}

	/*
	 * Overrides method from ViewerSorter
	 */
	public void sort(Viewer viewer, Object[] elements) {
		// Set label provider to show "path - resource"
		ISearchResultView view= SearchUI.getSearchResultView();
		if (view == null)
			return;
		fLabelProvider= view.getLabelProvider();
		if (fLabelProvider instanceof JavaSearchResultLabelProvider)
			((JavaSearchResultLabelProvider)fLabelProvider).setOrder(JavaSearchResultLabelProvider.SHOW_CONTAINER_ELEMENT);

		super.sort(viewer, elements);
	}
}
