/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.packageview;


import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;

/**
 * filters out all package fragments that have subpackages
 */
public class EmptyInnerPackageFilter extends PackageFilter {

	/**
	 * Returns the result of this filter, when applied to the
	 * given inputs.
	 *
	 * @param inputs the set of elements to 
	 * @return Returns true if element should be included in filtered set
	 */
	public boolean select(Viewer viewer, Object parent, Object element) {
		if (element instanceof IPackageFragment) {
			IPackageFragment pkg= (IPackageFragment)element;
			try {
				if (pkg.isDefaultPackage())
					return pkg.hasChildren();
				return !pkg.hasSubpackages() || pkg.hasChildren() || (pkg.getNonJavaResources().length > 0);
			} catch (JavaModelException e) {
				return false;
			}
		}
		return true;
	}
}