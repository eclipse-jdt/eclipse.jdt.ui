/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.packageview;


import org.eclipse.jface.viewers.IBasicPropertyConstants;
import org.eclipse.jface.viewers.ViewerFilter;


/**
 * Base class for package filters.
 */
public abstract class PackageFilter extends ViewerFilter {

	public boolean isFilterProperty(Object element, String property) {
		return IBasicPropertyConstants.P_TEXT.equals(property) || IBasicPropertyConstants.P_IMAGE.equals(property);
	}
}