package org.eclipse.jdt.internal.ui.packageview;import java.util.StringTokenizer;import java.util.Vector;import org.eclipse.core.resources.IProject;import org.eclipse.core.resources.IResource;import org.eclipse.core.runtime.IAdaptable;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.IPackageFragmentRoot;import org.eclipse.jdt.core.JavaCore;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.util.StringMatcher;import org.eclipse.jface.viewers.Viewer;import org.eclipse.jface.viewers.ViewerFilter;


/**
 * The LibraryFilter is a filter used to determine whether
 * a Java library is shown
 */
class LibraryFilter extends ViewerFilter {
	private boolean fShowLibraries;

	/**
	 * Creates a new library filter.
	 */
	public LibraryFilter() {
		super();
	}
	
	/**
	 * Returns whether libraries are shown.
	 */
	public boolean getShowLibraries() {
		return fShowLibraries;
	}
		
	/**
	 * Sets whether libraries are shown.
	 */
	public void setShowLibraries(boolean show) {
		fShowLibraries= show;
	}
	
	/* (non-Javadoc)
	 * Method declared on ViewerFilter.
	 */
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (fShowLibraries)
			return true;
		if (element instanceof IPackageFragmentRoot) {
			IPackageFragmentRoot root= (IPackageFragmentRoot)element;
			if (root.isArchive()) {
				// don't filter out JARs contained in the project itself
				IResource resource= null;
				try {
					resource= root.getUnderlyingResource();
					if (resource != null) {
						IProject jarProject= resource.getProject();
						IProject container= root.getJavaProject().getProject();
						return container.equals(jarProject);
					}
				} catch (JavaModelException e) {
					// fall through
				}
				return false;
			}
		}
		return true;
	}
}
