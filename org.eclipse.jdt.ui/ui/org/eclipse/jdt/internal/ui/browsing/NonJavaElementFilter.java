/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.browsing;

import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.ui.packageview.PackageFilter;

/**
 * Filters out all packages and folders
 */
public class NonJavaElementFilter  extends PackageFilter {
	
	private boolean fFilterContainers;
	
	/**
	 * Returns the result of this filter, when applied to the
	 * given inputs.
	 *
	 * @param inputs the set of elements to 
	 * @return Returns true if element should be included in filtered set
	 */
	public boolean select(Viewer viewer, Object parent, Object element) {
		return (element instanceof IJavaElement);
	}
}
