/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.reorg;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

public class ReorgUtils {
	
	//no instances
	private ReorgUtils(){
	}
	
	static Object getJavaParent(Object element) {
		if (element instanceof IResource) {
			IResource res= (IResource)element;
			IResource parent= (IResource)res.getParent();
			Object javaParent= JavaCore.create(parent);
			if (javaParent != null)
				return javaParent;
			return parent;
		} else if (element instanceof IJavaElement) {
			IJavaElement jElement= (IJavaElement)element;
			return jElement.getParent();
		}
		return null;
	}
	
	public static String getName(Object element) {
		if (element instanceof IResource) {
			IResource res= (IResource)element;
			return res.getName();
		} else if (element instanceof IJavaElement) {
			IJavaElement res= (IJavaElement)element;
			return res.getElementName();
		}
		return element.toString();
	}
	
	private static boolean shouldConfirmReadOnly(IResource res) {
		if (res.isReadOnly())
			return true;
		if (res instanceof IContainer) {
			IContainer container = (IContainer) res;
			try {
				IResource[] children = container.members();
				for (int i = 0; i < children.length; i++) {
					if (shouldConfirmReadOnly(children[i]))
						return true;
				}
			} catch (CoreException e) {
				// we catch this, we're only interested in knowing
				// whether to pop up the read-only dialog.
			}
		}
		return false;
	}


	// readonly confirmation
	public static boolean shouldConfirmReadOnly(Object element) {
		if (element instanceof IJavaElement) {
			try {
				if ((element instanceof IPackageFragmentRoot)
					&& isClasspathDelete((IPackageFragmentRoot) element)) {
					return false;
				}
				element = ((IJavaElement) element).getCorrespondingResource();
			} catch (JavaModelException e) {
				// we catch this, we're only interested in knowing
				// whether to pop up the read-only dialog.
			}
		}


		if (element instanceof IResource) {
			return shouldConfirmReadOnly((IResource) element);
		}
		return false;
	}
	
	static boolean isClasspathDelete(IPackageFragmentRoot pkgRoot) throws JavaModelException {
		IResource res= pkgRoot.getUnderlyingResource();
		if (res == null)
			return true;
		IProject definingProject= res.getProject();
		IProject occurringProject= pkgRoot.getJavaProject().getProject();
		return !definingProject.equals(occurringProject);
	}
}

