/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.workingsets;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.ui.IWorkingSet;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer;

/**
 * Working set filter for Java viewers.
 */
public class WorkingSetFilter extends ViewerFilter {

	private IWorkingSet fWorkingSet= null;
	private IAdaptable[] fCachedWorkingSet= null;

	/**
	 * Returns the working set which is used by this filter.
	 * 
	 * @return the working set
	 */
	public IWorkingSet getWorkingSet() {
		return fWorkingSet;
	}
		
	/**
	 * Sets this filter's working set.
	 * 
	 * @param workingSet the working set
	 */
	public void setWorkingSet(IWorkingSet workingSet) {
		fWorkingSet= workingSet;
	}
	
	/*
	 * Overrides method from ViewerFilter.
	 */
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (fWorkingSet == null || (fWorkingSet.isAggregateWorkingSet() && fWorkingSet.isEmpty()))
			return true;

		if (element instanceof IJavaElement)
			return isEnclosing((IJavaElement)element);

		if (element instanceof IResource)
			return isEnclosing(((IResource)element).getFullPath());
		
		if (element instanceof ClassPathContainer) {
			return isEnclosing((ClassPathContainer)element);
		}
			
		if (element instanceof IAdaptable) {
			IAdaptable adaptable= (IAdaptable)element;
			IJavaElement je= (IJavaElement)adaptable.getAdapter(IJavaElement.class);
			if (je != null)
				return isEnclosing(je);

			IResource resource= (IResource)adaptable.getAdapter(IResource.class);
			if (resource != null)
				return isEnclosing(resource.getFullPath());
		}

		return true;
	}

	private boolean isEnclosing(ClassPathContainer container) {
		// check whether the containing packagefragment root is enclosed
		Object[] roots= container.getPackageFragmentRoots();
		if (roots.length > 0)
			return isEnclosing((IPackageFragmentRoot)roots[0]);
		return false;
	}

	/*
 	 * Overrides method from ViewerFilter
 	 */
	public Object[] filter(Viewer viewer, Object parent, Object[] elements) {
		Object[] result= null;
		if (fWorkingSet != null) 
			fCachedWorkingSet= fWorkingSet.getElements();
		try {
			result= super.filter(viewer, parent, elements);
		} finally {
			fCachedWorkingSet= null;
		}
		return result;
	}

	private boolean isEnclosing(IPath elementPath) {
		if (elementPath == null)
			return false;
			
		IAdaptable[] cachedWorkingSet= fCachedWorkingSet;
		if (cachedWorkingSet == null)
			cachedWorkingSet= fWorkingSet.getElements();

		int length= cachedWorkingSet.length;
		for (int i= 0; i < length; i++) {
			if (isEnclosing(cachedWorkingSet[i], elementPath))
				return true;
		}
		return false;
	}
	
	public boolean isEnclosing(IJavaElement element) {
		Assert.isNotNull(element);
		
		IAdaptable[] cachedWorkingSet= fCachedWorkingSet;
		if (cachedWorkingSet == null)
			cachedWorkingSet= fWorkingSet.getElements();
		
		boolean isElementPathComputed= false;
		IPath elementPath= null; // will be lazy computed if needed
		
		int length= cachedWorkingSet.length;
		for (int i= 0; i < length; i++) {
			IJavaElement scopeElement= (IJavaElement)cachedWorkingSet[i].getAdapter(IJavaElement.class);
			if (scopeElement != null) {
				// compare Java elements
				IJavaElement searchedElement= element;
				while (searchedElement != null) {
					if (searchedElement.equals(scopeElement))
						return true;
					else {
						if (scopeElement.getElementType() == IJavaElement.JAVA_PROJECT && searchedElement.getElementType() == IJavaElement.PACKAGE_FRAGMENT_ROOT) {
							IPackageFragmentRoot pkgRoot= (IPackageFragmentRoot)searchedElement;
							if (pkgRoot.isExternal() && pkgRoot.isArchive()) {
								if (((IJavaProject)scopeElement).isOnClasspath(searchedElement))
									return true;
						}
						}
						searchedElement= searchedElement.getParent();
						if (searchedElement != null && searchedElement.getElementType() == IJavaElement.COMPILATION_UNIT) {
							ICompilationUnit unit= (ICompilationUnit)searchedElement;
							unit= unit.getPrimary();
						}
					}
				}
				while (scopeElement != null) {
					if (element.equals(scopeElement))
						return true;
					else
						scopeElement= scopeElement.getParent();
				}
			} else {
				// compare resource paths
				if (!isElementPathComputed) {
					IResource elementResource= (IResource)element.getAdapter(IResource.class);
					if (elementResource != null)
						elementPath= elementResource.getFullPath();
				}
				if (isEnclosing(cachedWorkingSet[i], elementPath))
					return true;
			}
		}
		return false;
	}
	
	private boolean isEnclosing(IAdaptable element, IPath path) {
		if (path == null)
			return false;
		
		IPath elementPath= null;
		
		IResource elementResource= (IResource)element.getAdapter(IResource.class);
		if (elementResource != null)
			elementPath= elementResource.getFullPath();

		if (elementPath == null) {
			IJavaElement javaElement= (IJavaElement)element.getAdapter(IJavaElement.class);
			if (javaElement != null)
				elementPath= javaElement.getPath();
		}

		if (elementPath == null && element instanceof IStorage)
			elementPath= ((IStorage)element).getFullPath();
		
		if (elementPath == null)			
			return false;

		if (elementPath.isPrefixOf(path))
			return true;

		if (path.isPrefixOf(elementPath))
			return true;
		
		return false;
	}
	
}
