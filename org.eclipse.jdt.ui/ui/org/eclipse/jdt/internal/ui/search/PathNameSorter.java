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

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.search.ui.ISearchResultViewEntry;

/**
 * Sorts the search result viewer by the path name.
 */
public class PathNameSorter extends JavaSearchSorter {

	/*
	 * Overrides method from ViewerSorter
	 */
	public int compare(Viewer viewer, Object e1, Object e2) {
		String name1= null;
		String name2= null;
		ISearchResultViewEntry entry1= null;
		ISearchResultViewEntry entry2= null;

		if (e1 instanceof ISearchResultViewEntry) {
			entry1= (ISearchResultViewEntry)e1;
			name1= getLabel(e1);
		}
		if (e2 instanceof ISearchResultViewEntry) {
			entry2= (ISearchResultViewEntry)e2;
			name2= getLabel(e2);
		}
		if (name1 == null)
			name1= ""; //$NON-NLS-1$
		if (name2 == null)
			name2= ""; //$NON-NLS-1$
			
		IResource resource= null;
		if (entry1 != null)
			resource= entry1.getResource();
		if (resource != null && entry2 != null && resource == entry2.getResource()) {

			if (resource instanceof IProject || resource.getFileExtension().equalsIgnoreCase("jar") || resource.getFileExtension().equalsIgnoreCase("zip")) //$NON-NLS-2$ //$NON-NLS-1$
				// binary archives
				return getCollator().compare(name1, name2);

			// Sort by marker start position if resource is equal.			
			int startPos1= -1;
			int startPos2= -1;
			IMarker marker1= entry1.getSelectedMarker();
			IMarker marker2= entry2.getSelectedMarker();

			if (marker1 != null)
				startPos1= marker1.getAttribute(IMarker.CHAR_START, -1);
			if (marker2 != null)
			 	startPos2= marker2.getAttribute(IMarker.CHAR_START, -1);
			return startPos1 - startPos2;
		}
		
		return getCollator().compare(name1, name2);
	}

	protected int getLabelAppearance() {
		return JavaSearchResultLabelProvider.SHOW_PATH;
	}
}

