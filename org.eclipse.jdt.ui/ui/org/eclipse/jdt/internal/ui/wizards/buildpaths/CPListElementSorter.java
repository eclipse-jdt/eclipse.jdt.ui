package org.eclipse.jdt.internal.ui.wizards.buildpaths;import org.eclipse.core.runtime.IPath;import org.eclipse.jface.viewers.Viewer;import org.eclipse.jface.viewers.ViewerSorter;import org.eclipse.jdt.core.IClasspathEntry;

public class CPListElementSorter extends ViewerSorter {
	
	/**
	 * @see ViewerSorter#category(Object)
	 */
	public int category(Object obj) {
		if (obj instanceof CPListElement) {
			switch (((CPListElement)obj).getEntryKind()) {
			case IClasspathEntry.CPE_LIBRARY:
				return 3;
			case IClasspathEntry.CPE_PROJECT:
				return 1;
			case IClasspathEntry.CPE_SOURCE:
				return 0;
			case IClasspathEntry.CPE_VARIABLE:
				return 2;
			}
		}
		return super.category(obj);
	}
	
	public int compare(Viewer viewer, Object e1, Object e2) {
		int cat1 = category(e1);
		int cat2 = category(e2);
	
		if (cat1 != cat2)
			return cat1 - cat2;
			
		if ((e1 instanceof CPListElement) && (e2 instanceof CPListElement)) {
			IPath p1= ((CPListElement)e1).getPath();
			IPath p2= ((CPListElement)e1).getPath();
			
			return p1.toString().compareTo(p2.toString());
		}
		return super.compare(viewer, e1, e2);
	}	

}
