package org.eclipse.jdt.internal.ui;/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */


import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

/**
 * This class locates different resources
 * which are related to an object
 */
public class ResourceLocator implements IResourceLocator {
	/* 
       * Implements a method from IResourceLocator
       */
       public IResource getUnderlyingResource(Object element) throws JavaModelException {
		if (element instanceof IJavaElement)
			return ((IJavaElement)element).getUnderlyingResource();
		else
			return null;
	}

	/* 
       * Implements a method from IResourceLocator
       */
	public IResource getCorrespondingResource(Object element) throws JavaModelException {
		if (element instanceof IJavaElement)
			return ((IJavaElement)element).getUnderlyingResource();
		else
			return null;
	}

	/*
       * Implements a method from IResourceLocator
       */
	public IResource getContainingResource(Object element) throws JavaModelException {
		IResource resource= null;
		if (element instanceof IResource)
			resource= (IResource)element;
		if (element instanceof IJavaElement) {
			resource= ((IJavaElement)element).getUnderlyingResource();
			if (resource == null)
				resource= ((IJavaElement)element).getJavaProject().getProject();
		}
		return resource;
	}
}