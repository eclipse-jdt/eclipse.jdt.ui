/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.search;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.search.ui.ISearchResultViewEntry;
import org.eclipse.ui.texteditor.MarkerUtilities;

/**
 * Sorts the search result viewer by the matching position.
 */
public class MatchPositionSorter extends ViewerSorter {
	/*
	 * Overrides method from ViewerSorter
	 */
	public int compare(Viewer viewer, Object e1, Object e2) {
		int pos1= 0;
		int pos2= 0;

		if (e1 instanceof ISearchResultViewEntry)
			pos1= getMatchPosition((ISearchResultViewEntry)e1);
		if (e2 instanceof ISearchResultViewEntry)
			pos2= getMatchPosition((ISearchResultViewEntry)e2);
		return pos1-pos2;
	}
	
	private int getMatchPosition(ISearchResultViewEntry entry) {
		IMarker marker= entry.getSelectedMarker();
		if (marker == null)
			return 0;
		return MarkerUtilities.getCharStart(marker);
	}

	/*
	 * Overrides method from ViewerSorter
	 */
	public boolean isSorterProperty(Object element, String property) {
		return true;
	}
}
