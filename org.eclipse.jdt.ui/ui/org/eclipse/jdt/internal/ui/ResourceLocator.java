/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

/**
 * This class locates different resources
 * which are related to an object
 */
public class ResourceLocator implements IResourceLocator {
	
	public IResource getUnderlyingResource(Object element) throws JavaModelException {
		if (element instanceof IJavaElement)
			return ((IJavaElement) element).getUnderlyingResource();
		else
			return null;
	}

	public IResource getCorrespondingResource(Object element) throws JavaModelException {
		if (element instanceof IJavaElement)
			return ((IJavaElement) element).getUnderlyingResource();
		else
			return null;
	}

	public IResource getContainingResource(Object element) throws JavaModelException {
		IResource resource= null;
		if (element instanceof IResource)
			resource= (IResource) element;
		if (element instanceof IJavaElement) {
			resource= ((IJavaElement) element).getUnderlyingResource();
			if (resource == null)
				resource= ((IJavaElement) element).getJavaProject().getProject();
		}
		return resource;
	}
}