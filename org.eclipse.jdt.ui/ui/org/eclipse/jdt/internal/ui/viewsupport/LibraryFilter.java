/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.viewsupport;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.jdt.core.IPackageFragmentRoot;

/**
 * Filters out all elements which libraries
 */
public class LibraryFilter extends ViewerFilter {

	/**
	 * Returns the result of this filter, when applied to the
	 * given inputs.
	 *
	 * @param inputs the set of elements to 
	 * @return Returns true if element should be included in filtered set
	 */
	public boolean select(Viewer viewer, Object parent, Object element) {
		if (element instanceof IPackageFragmentRoot)
			return !((IPackageFragmentRoot)element).isArchive();
		return true;
	}
}