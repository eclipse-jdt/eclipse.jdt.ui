/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

package org.eclipse.jdt.internal.ui.reorg;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceManipulation;
import org.eclipse.jdt.core.JavaModelException;

public class DeleteSupport implements IDeleteSupport {
	public boolean canDelete(Object o) {
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

	public void delete(Object o, IProgressMonitor pm) throws JavaModelException, CoreException {
		if (o instanceof ISourceManipulation) {
			ISourceManipulation element= (ISourceManipulation)o;
			element.delete(true, pm);
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
			res.delete(true, pm);
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
}