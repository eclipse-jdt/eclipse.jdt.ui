/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp. and others.
All rights reserved.   This program and the accompanying materials
are made available under the terms of the Common Public License v0.5
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v05.html
 
Contributors:
	Daniel Megert - Initial implementation
**********************************************************************/

package org.eclipse.jdt.internal.ui.workingsets;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.ui.IWorkingSet;

import org.eclipse.jdt.core.IJavaElement;
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
		if (fWorkingSet == null)
			return true;

		if (element instanceof IJavaElement)
			return isEnclosing((IJavaElement)element);

		if (element instanceof IResource)
			return isEnclosing(((IResource)element).getFullPath());
			
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
	
	private boolean isEnclosing(IJavaElement element) {
		IAdaptable[] cachedWorkingSet= fCachedWorkingSet;
		if (cachedWorkingSet == null)
			cachedWorkingSet= fWorkingSet.getElements();
		
		boolean isElementPathComputed= false;
		IPath elementPath= null; // will be lazy computed if needed
		
		int length= cachedWorkingSet.length;
		for (int i = 0; i < length; i++) {
			IJavaElement scopeElement= (IJavaElement)cachedWorkingSet[i].getAdapter(IJavaElement.class);
			if (scopeElement != null) {
				// compare Java elements
				IJavaElement searchedElement= element;
				while (scopeElement != null && searchedElement != null) {
					if (searchedElement.equals(scopeElement))
						return true;
					else
						searchedElement = searchedElement.getParent();
				}
				while (scopeElement != null && element != null) {
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