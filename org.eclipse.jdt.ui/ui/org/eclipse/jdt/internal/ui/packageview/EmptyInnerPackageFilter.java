/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.packageview;


import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;

/**
 * Filters empty non-leaf package fragments
 */
public class EmptyInnerPackageFilter extends PackageFilter {

	/*
	 * @see ViewerFilter
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
		// XXX: revisit, temporary fix to filter package declaration
		if (element instanceof IPackageDeclaration)
			return false;
		return true;
	}
}