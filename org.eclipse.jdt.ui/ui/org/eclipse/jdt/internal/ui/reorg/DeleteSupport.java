/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.reorg;

import org.eclipse.core.resources.IFile;import org.eclipse.core.resources.IProject;import org.eclipse.core.resources.IResource;import org.eclipse.core.runtime.CoreException;import org.eclipse.core.runtime.IPath;import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.core.runtime.SubProgressMonitor;import org.eclipse.jdt.core.IClasspathEntry;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.IJavaProject;import org.eclipse.jdt.core.IPackageFragment;import org.eclipse.jdt.core.IPackageFragmentRoot;import org.eclipse.jdt.core.ISourceManipulation;import org.eclipse.jdt.core.JavaCore;import org.eclipse.jdt.core.JavaModelException;

public class DeleteSupport implements IDeleteSupport {
	public boolean canDelete(Object o) {
		try {
			if (o instanceof IPackageFragmentRoot && 
					ReorgSupport.isClasspathDelete((IPackageFragmentRoot)o)) {
						return true;
			}
		} catch (CoreException e) {
			// we can't delete.
			return false;
		}
		if (o instanceof IPackageFragment) {
			IPackageFragment fragment= (IPackageFragment)o;
			if (fragment.isDefaultPackage())
				return false;
		}
		if (o instanceof IJavaElement) {
			IJavaElement element= (IJavaElement)o;
			try {
				IResource res= element.getCorrespondingResource();
				if (res == null)
					return false;
				if (!res.getProject().equals(element.getJavaProject().getProject()))
					return false;
			} catch (JavaModelException e) {
				return false;
			}
			IJavaElement parent= element.getParent();
			return parent == null || !parent.isReadOnly();
		}
		
		if (o instanceof IFile) {
			Object parent= ReorgSupportFactory.getJavaParent(o);
			if (parent instanceof IJavaElement) {
				return !((IJavaElement)parent).isReadOnly();
			}
			return parent != null;
		};
		return o instanceof IResource;
	}

	public void delete(Object o, boolean deleteProjectContent, IProgressMonitor pm) throws JavaModelException, CoreException {
		IProgressMonitor subPM= new SubProgressMonitor(pm, 10);
		if (o instanceof IPackageFragmentRoot) {
			IPackageFragmentRoot root= (IPackageFragmentRoot)o;
			if (ReorgSupport.isClasspathDelete(root)) {
				deleteFromClasspath(root, subPM);
			}
		}
		pm.subTask(getElementName(o));
		if (o instanceof ISourceManipulation) {
			ISourceManipulation element= (ISourceManipulation)o;
			element.delete(true, subPM);
			return;
		}
		if (o instanceof IJavaElement) {
			IJavaElement element= (IJavaElement)o;
			if (!element.exists())
				return;
			o= element.getCorrespondingResource();
		}
		if (o instanceof IResource) {
			IResource res= (IResource)o;
			if (!res.exists())
				return;
			// 1GEZU7T: ITPJUI:ALL - Track workbench changes to DeleteAction
			if (res instanceof IProject) {
				((IProject)res).delete(deleteProjectContent, true, subPM);
			} else {
				res.delete(true, subPM);
			}
		}
	}
	
	public int getPathLength(Object o) {
		if (o instanceof IJavaElement) {
			try {
				o= ((IJavaElement)o).getUnderlyingResource();
			} catch (JavaModelException e) {
			}
		}
		if (o instanceof IResource) {
			return ((IResource)o).getFullPath().segmentCount();
		}
		return 0;
	}
	
	public String getElementName(Object element) {
		if (element instanceof IJavaElement) {
			return ((IJavaElement)element).getElementName();
		} else if (element instanceof IResource) {
			return ((IResource)element).getName();
		}
		return null;
	}
	
	private void deleteFromClasspath(IPackageFragmentRoot root, IProgressMonitor pm) throws JavaModelException {
		pm.subTask(getElementName(root)+ReorgMessages.getString("DeleteSupport.removeFromClasspath.task")); //$NON-NLS-1$
		IPath path= root.getPath();
		IJavaProject project= root.getJavaProject();
		IClasspathEntry[] cp= project.getRawClasspath();
		IClasspathEntry[] newCp= new IClasspathEntry[cp.length-1];
		int i= 0; 
		int j= 0;
		
		while (j < newCp.length) {
			if (path.equals(JavaCore.getResolvedClasspathEntry(cp[i]).getPath())) {
				i++;
			} 

			newCp[j]= cp[i];
			i++;
			j++;
		} 
		project.setRawClasspath(newCp, pm);
	}
}