/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.packageview;


import org.eclipse.jface.viewers.IBasicPropertyConstants;
import org.eclipse.jface.viewers.ViewerFilter;


/**
 * filters out all package fragments that have subpackages
 */
public abstract class PackageFilter extends ViewerFilter {

	/**
	 * Returns true if I am effected by the property change.
	 * @param element the element who's property has changed.
	 * @param property the property that has changed.
	 */
	public boolean isFilterProperty(Object element, String property) {
		return IBasicPropertyConstants.P_TEXT.equals(property) || IBasicPropertyConstants.P_IMAGE.equals(property);
	}
}