/**********************************************************************Copyright (c) 2000, 2002 IBM Corp. and others.All rights reserved.   This program and the accompanying materialsare made available under the terms of the Common Public License v0.5which accompanies this distribution, and is available athttp://www.eclipse.org/legal/cpl-v05.html Contributors:	Daniel Megert - Initial implementation**********************************************************************/package org.eclipse.jdt.internal.ui.workingsets;import org.eclipse.core.resources.IResource;import org.eclipse.core.resources.IStorage;import org.eclipse.core.runtime.IAdaptable;import org.eclipse.core.runtime.IPath;import org.eclipse.core.runtime.Path;import org.eclipse.jface.viewers.Viewer;import org.eclipse.jface.viewers.ViewerFilter;import org.eclipse.ui.IWorkingSet;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.IOpenable;import org.eclipse.jdt.core.IPackageFragment;import org.eclipse.jdt.core.IPackageFragmentRoot;
/**
 * Working set filter for Java viewers.
 */
public class WorkingSetFilter extends ViewerFilter {
	private IWorkingSet fWorkingSet= null;	private IAdaptable[] fCachedWorkingSet= null;

	/**
	 * Returns the working set which is used by this filter.	 * 	 * @return the working set
	 */
	public IWorkingSet getWorkingSet() {
		return fWorkingSet;
	}
		
	/**
	 * Sets this filter's working set.	 * 	 * @param workingSet the working set
	 */
	public void setWorkingSet(IWorkingSet workingSet) {
		fWorkingSet= workingSet;
	}
	
	/*
	 * Implements method from ViewerFilter.
	 */
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (fWorkingSet == null)
			return true;		if (element instanceof IJavaElement)			return encloses((IJavaElement)element);		if (element instanceof IResource)			return encloses(((IResource)element).getFullPath());					if (element instanceof IAdaptable) {			IJavaElement je= (IJavaElement)((IAdaptable)element).getAdapter(IJavaElement.class);			if (je != null)				return encloses(je);		}		if (element instanceof IStorage)			return encloses(((IStorage)element).getFullPath());		return true;
	}
	/* 	 * Overrides method from ViewerFilter 	 */	public Object[] filter(Viewer viewer, Object parent, Object[] elements) {		Object[] result= null;		if (fWorkingSet != null) 			fCachedWorkingSet= fWorkingSet.getElements();		try {			result= super.filter(viewer, parent, elements);		} finally {			fCachedWorkingSet= null;		}		return result;	}	private boolean encloses(IPath elementPath) {		if (elementPath == null)			return false;					IAdaptable[] cachedWorkingSet= fCachedWorkingSet;		if (cachedWorkingSet == null)			cachedWorkingSet= fWorkingSet.getElements();		int length= cachedWorkingSet.length;		for (int i= 0; i < length; i++) {			IResource scopeElement= (IResource)cachedWorkingSet[i].getAdapter(IResource.class);			if (scopeElement == null)				continue;			IPath scopeElementPath= scopeElement.getFullPath();			if (elementPath.isPrefixOf(scopeElementPath))				return true;			if (scopeElementPath.isPrefixOf(elementPath))				return true;		}		return false;	}		private boolean encloses(IJavaElement element) {		IAdaptable[] cachedWorkingSet= fCachedWorkingSet;		if (cachedWorkingSet == null)			cachedWorkingSet= fWorkingSet.getElements();				int length= cachedWorkingSet.length;		for (int i = 0; i < length; i++) {			IJavaElement scopeElement= (IJavaElement)cachedWorkingSet[i].getAdapter(IJavaElement.class);			IJavaElement searchedElement= element;			while (scopeElement != null && searchedElement != null) {				if (searchedElement.equals(scopeElement))					return true;				else					searchedElement = searchedElement.getParent();			}			while (scopeElement != null && element != null) {				if (element.equals(scopeElement))					return true;				else					scopeElement= scopeElement.getParent();			}		}		Object resource= element.getAdapter(IResource.class);		if (resource instanceof IResource)			return encloses(((IResource)resource).getFullPath());		return false;	}}