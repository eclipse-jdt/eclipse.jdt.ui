/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.reorg;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;

public class ReorgUtils {
	
	//no instances
	private ReorgUtils(){
	}
	
	static Object getJavaParent(Object element) {
		if (element instanceof IResource) {
			IResource res= (IResource)element;
			IResource parent= res.getParent();
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
				element = ((IJavaElement) element).getResource();
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
		IResource res= pkgRoot.getResource();
		if (res == null)
			return true;
		IProject definingProject= res.getProject();
		IProject occurringProject= pkgRoot.getJavaProject().getProject();
		return !definingProject.equals(occurringProject);
	}

	public static boolean isParent(IPackageFragment pack, IPackageFragmentRoot root){
		if (pack == null)
			return false;		
		IJavaElement packParent= pack.getParent();
		if (packParent == null)
			return false;		
		if (packParent.equals(root))	
			return true;
		IResource packageResource= ResourceUtil.getResource(pack);
		IResource packageRootResource= ResourceUtil.getResource(root);
		return isParent(packageResource, packageRootResource);
	}

	public static boolean isParent(ICompilationUnit cu, IPackageFragment dest){
		if (cu == null)
			return false;
		IJavaElement cuParent= cu.getParent();
		if (cuParent == null)
			return false;
		if (cuParent.equals(dest))	
			return true;
		IResource cuResource= ResourceUtil.getResource(cu);
		IResource packageResource= ResourceUtil.getResource(dest);
		return isParent(cuResource, packageResource);
	}

	public static boolean isParent(IResource res, IResource maybeParent){
		if (res == null)
			return false;
		return equalInWorkspaceOrOnDisk(res.getParent(), maybeParent);
	}
	
	public static boolean equalInWorkspaceOrOnDisk(IResource r1, IResource r2){
		if (r1 == null || r2 == null)
			return false;
		if (r1.equals(r2))
			return true;
		IPath r1Location= r1.getLocation();
		IPath r2Location= r2.getLocation();
		if (r1Location == null || r2Location == null)
			return false;
		return r1Location.equals(r2Location);
	}
}

