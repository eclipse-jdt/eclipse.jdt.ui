package org.eclipse.jdt.internal.ui;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.ui.views.tasklist.ITaskListResourceAdapter;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

public class JavaTaskListAdapter implements ITaskListResourceAdapter {
	/*
	 * @see ITaskListResourceAdapter#getAffectedResource(IAdaptable)
	 */
	public IResource getAffectedResource(IAdaptable element) {
		IJavaElement java = (IJavaElement) element;
		try {
			IResource resource= java.getResource();
			if (resource != null)
				return resource; 

			ICompilationUnit cu= (ICompilationUnit) java.getAncestor(IJavaElement.COMPILATION_UNIT);
			if (cu != null) {
				return JavaModelUtil.toOriginal(cu).getResource();
			}
		} catch (JavaModelException e) {
		}
		return null;
	 }
}

