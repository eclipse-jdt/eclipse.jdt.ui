/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.jarpackager;

import java.util.List;
import org.eclipse.core.resources.IContainer;

import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.ui.packageview.PackageFilter;

/**
 * Filters out all packages and folders
 */
public class SealPackagesFilter  extends PackageFilter {

	List fAllowedPackages;	

	public SealPackagesFilter(List packages) {
		fAllowedPackages= packages;
	}

	/**
	 * Returns the result of this filter, when applied to the
	 * given inputs.
	 *
	 * @param inputs the set of elements to 
	 * @return Returns true if element should be included in filtered set
	 */
	public boolean select(Viewer viewer, Object parent, Object element) {
		if (element instanceof IJavaElement) {
			int type= ((IJavaElement)element).getElementType();
			if (type == IJavaElement.JAVA_MODEL || type == IJavaElement.JAVA_PROJECT || type ==IJavaElement.PACKAGE_FRAGMENT_ROOT)
				return true;
			return (type == IJavaElement.PACKAGE_FRAGMENT && fAllowedPackages.contains(element));
			
		}
		else
			return false;
	}
}
