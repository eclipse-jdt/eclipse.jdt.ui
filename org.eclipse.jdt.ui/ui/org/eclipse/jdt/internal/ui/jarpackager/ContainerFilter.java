/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.jarpackager;

import org.eclipse.core.resources.IContainer;

import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.ui.filters.AbstractFilter;

/**
 * Filters out all packages and folders
 */
class ContainerFilter  extends AbstractFilter {
	
	private boolean fFilterContainers;
	
	public static boolean FILTER_CONTAINERS= true;
	public static boolean FILTER_NON_CONTAINERS= false;

	public ContainerFilter(boolean filterContainers) {
		fFilterContainers= filterContainers;
	}
	
	/**
	 * Returns the result of this filter, when applied to the
	 * given inputs.
	 *
	 * @param inputs the set of elements to 
	 * @return Returns true if element should be included in filtered set
	 */
	public boolean select(Viewer viewer, Object parent, Object element) {
		boolean isContainer= element instanceof IContainer;
		if (!isContainer && element instanceof IJavaElement) {
			int type= ((IJavaElement)element).getElementType();
			isContainer= type == IJavaElement.JAVA_MODEL
						|| type == IJavaElement.JAVA_PROJECT
						|| type == IJavaElement.PACKAGE_FRAGMENT
						|| type ==IJavaElement.PACKAGE_FRAGMENT_ROOT;
		}
		return (fFilterContainers && !isContainer) || (!fFilterContainers && isContainer);
	}
}
