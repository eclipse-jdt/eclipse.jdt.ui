/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
package org.eclipse.jdt.internal.ui;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.ui.IContributorResourceAdapter;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

public class JavaContributorResourceAdapter implements IContributorResourceAdapter {

	/*
	 * @see IContributorResourceAdapter#getAdaptedResource(IAdaptable)
	 */
	public IResource getAdaptedResource(IAdaptable element) {
		/*
		 * Make actions on ICompilationUnit available on IType
		 */
		if (element instanceof IType) {
			IType type= (IType)element;
			IJavaElement parent= type.getParent();
			if (parent instanceof ICompilationUnit) {
				ICompilationUnit cu= (ICompilationUnit)parent;
				if (cu.isWorkingCopy())
					element= cu.getOriginalElement();
				else 
					element= cu;
			}
		}
		try {
			return ((IJavaElement)element).getCorrespondingResource();
		} catch (JavaModelException e) {
			return null;	
		}
	}
}

