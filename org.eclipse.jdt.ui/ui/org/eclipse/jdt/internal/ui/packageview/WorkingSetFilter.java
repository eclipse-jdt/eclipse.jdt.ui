/* * (c) Copyright IBM Corp. 2000, 2001. * All Rights Reserved. */package org.eclipse.jdt.internal.ui.packageview;import java.util.StringTokenizer;import java.util.Vector;import org.eclipse.core.resources.IProject;import org.eclipse.core.resources.IResource;import org.eclipse.core.runtime.IAdaptable;import org.eclipse.core.runtime.IPath;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.IPackageFragmentRoot;import org.eclipse.jdt.core.JavaCore;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.util.StringMatcher;import org.eclipse.jface.viewers.Viewer;import org.eclipse.jface.viewers.ViewerFilter;import org.eclipse.search.ui.IWorkingSet;


/**
 * A viewer filter for a working set.
 */
class WorkingSetFilter extends ViewerFilter {
	private IWorkingSet fWorkingSet= null;	private IResource[] fCachedWorkingSet= null;

	/**
	 * Returns the active working set.
	 */
	public IWorkingSet getWorkingSet() {
		return fWorkingSet;
	}
		
	/**
	 * Sets the active working set.
	 */
	public void setWorkingSet(IWorkingSet workingSet) {
		fWorkingSet= workingSet;
	}
	
	/* (non-Javadoc)
	 * Method declared on ViewerFilter.
	 */
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (fWorkingSet == null)
			return true;					if (element instanceof IAdaptable) {			IAdaptable adaptable= (IAdaptable) element;			Object resource= adaptable.getAdapter(IResource.class);			if (resource instanceof IResource) 				return isEnclosed((IResource)resource);		}
		return true;
	}
	boolean isEnclosed(IResource element) {		IPath elementPath= element.getFullPath();		for (int i = 0; i < fCachedWorkingSet.length; i++) {
			IResource resource= fCachedWorkingSet[i];			IPath resourcePath= resource.getFullPath();			if (resourcePath.isPrefixOf(elementPath))				return true;			if (elementPath.isPrefixOf(resourcePath))				return true;		}		return false;	}		/* 	* @see ViewerFilter#filter(Viewer, Object, Object[]) 	*/	public Object[] filter(Viewer viewer, Object parent, Object[] elements) {		Object[] result= null;		if (fWorkingSet != null) 			fCachedWorkingSet= fWorkingSet.getResources();		try {			result= super.filter(viewer, parent, elements);		} finally {			fCachedWorkingSet= null;		}		return result;	}
}