package org.eclipse.jdt.internal.ui.packageview;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * Representation of class path containers in Java UI.
 */
public class ClassPathContainer implements IAdaptable, IWorkbenchAdapter {
	private IJavaProject fProject;
	private IClasspathEntry fClassPathEntry;
	private IClasspathContainer fContainer;

	public ClassPathContainer(IJavaProject parent, IClasspathEntry entry) {
		fProject= parent;
		fClassPathEntry= entry;
		try {
			fContainer= JavaCore.getClasspathContainer(entry.getPath(), parent);
		} catch (JavaModelException e) {
			fContainer= null;
		}
	}

	public boolean equals(Object obj) {
		if (obj instanceof ClassPathContainer) {
			ClassPathContainer other = (ClassPathContainer)obj;
			if (fProject.equals(other.fProject) &&
				fClassPathEntry.equals(other.fClassPathEntry)) {
				return true;	
			}
			
		}
		return false;
	}

	public int hashCode() {
		return fProject.hashCode()*17+fClassPathEntry.hashCode();
	}

	public Object[] getPackageFragmentRoots() {
		return fProject.findPackageFragmentRoots(fClassPathEntry);
	}

	public Object getAdapter(Class adapter) {
		if (adapter == IWorkbenchAdapter.class) 
			return this;
		return null;
	}

	public Object[] getChildren(Object o) {
		return getPackageFragmentRoots();
	}

	public ImageDescriptor getImageDescriptor(Object object) {
		return JavaPluginImages.DESC_OBJS_LIBRARY;
	}

	public String getLabel(Object o) {
		if (fContainer != null)
			return fContainer.getDescription();
		return PackagesMessages.getString("ClassPathContainer.error_label"); //$NON-NLS-1$
	}

	public Object getParent(Object o) {
		return getJavaProject();
	}

	public IJavaProject getJavaProject() {
		return fProject;
	}

	static boolean contains(IJavaProject project, IClasspathEntry entry, IPackageFragmentRoot root) {
		IPackageFragmentRoot[] roots= project.findPackageFragmentRoots(entry);
		for (int i= 0; i < roots.length; i++) {
			if (roots[i].equals(root))
				return true;
		}
		return false;
	}
}
