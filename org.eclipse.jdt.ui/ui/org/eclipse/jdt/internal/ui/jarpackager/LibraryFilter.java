/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.jarpackager;

import org.eclipse.core.resources.IContainer;

import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.packageview.PackageFilter;

/**
 * Filters out all elements which libraries
 */
public class LibraryFilter extends PackageFilter {
	/**
	 * Returns the result of this filter, when applied to the
	 * given inputs.
	 *
	 * @param inputs the set of elements to 
	 * @return Returns true if element should be included in filtered set
	 */
	public boolean select(Viewer viewer, Object parent, Object element) {
		if (element instanceof IPackageFragmentRoot) {
			try {
				if (((IPackageFragmentRoot)element).getKind() != IPackageFragmentRoot.K_SOURCE)
					return false;
			} catch (JavaModelException ex) {
				return true;
			}
		}
		return true;
	}
}