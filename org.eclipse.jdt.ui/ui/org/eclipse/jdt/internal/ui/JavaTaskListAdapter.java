package org.eclipse.jdt.internal.ui;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.ui.views.tasklist.ITaskListResourceAdapter;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

public class JavaTaskListAdapter implements ITaskListResourceAdapter {
	/*
	 * @see ITaskListResourceAdapter#getAffectedResource(IAdaptable)
	 */
	public IResource getAffectedResource(IAdaptable element) {
		IJavaElement java = (IJavaElement) element;
		try {
			IResource resource= java.getCorrespondingResource();
			if (resource != null)
				return resource; 

			ICompilationUnit cu= (ICompilationUnit) java.getAncestor(IJavaElement.COMPILATION_UNIT);
			if (cu != null) {
				if (cu.isWorkingCopy())
					return cu.getOriginalElement().getUnderlyingResource();
				return cu.getUnderlyingResource();
			}
		} catch (JavaModelException e) {
		}
		return null;
	 }
}

