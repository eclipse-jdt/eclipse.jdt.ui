/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.packageview;

import java.util.StringTokenizer;
import java.util.Vector;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.StringMatcher;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;


/**
 * The BinaryProjectFilter is a filter used to determine whether
 * a Java project is shown
 */
class BinaryProjectFilter extends ViewerFilter {
	private boolean fShowBinaryProjects;

	/**
	 * Creates a new library filter.
	 */
	public BinaryProjectFilter() {
		super();
	}
	
	/**
	 * Returns whether libraries are shown.
	 */
	public boolean getShowBinaries() {
		return fShowBinaryProjects;
	}
		
	/**
	 * Sets whether binary projects are shown.
	 */
	public void setShowBinaries(boolean show) {
		fShowBinaryProjects= show;
	}
	
	/* (non-Javadoc)
	 * Method declared on ViewerFilter.
	 */
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (fShowBinaryProjects)
			return true;
		if (element instanceof IJavaProject) 
			return !isBinaryProject((IJavaProject)element);
		return true;
	}
	
	boolean isBinaryProject(IJavaProject p) {
		try {
			IClasspathEntry[] entries= p.getRawClasspath();
			for (int i= 0; i < entries.length; i++) {
				if (entries[i].getEntryKind() == IClasspathEntry.CPE_SOURCE)
					return false;
			}
		} catch (JavaModelException e) {
			// fall through
		}
		return true;
	}

}
