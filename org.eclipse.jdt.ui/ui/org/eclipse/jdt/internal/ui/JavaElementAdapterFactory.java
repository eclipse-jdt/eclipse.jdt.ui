package org.eclipse.jdt.internal.ui;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */


import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterFactory;

import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.views.properties.IPropertySource;

import org.eclipse.search.ui.ISearchPageScoreComputer;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.search.JavaSearchPageScoreComputer;

/**
 * Implements basic UI support for Java elements.
 * Implements handle to persistent support for Java elements.
 */
public class JavaElementAdapterFactory implements IAdapterFactory {
	
	private static Class[] PROPERTIES= new Class[] {
		IPropertySource.class,
		IResource.class,
		ISearchPageScoreComputer.class,
		IWorkbenchAdapter.class,
		IResourceLocator.class
	};
	
	private ISearchPageScoreComputer fSearchPageScoreComputer= new JavaSearchPageScoreComputer();
	private static IResourceLocator fgResourceLocator= new ResourceLocator();
	private static JavaWorkbenchAdapter fgJavaWorkbenchAdapter= new JavaWorkbenchAdapter();
 
	
	public Class[] getAdapterList() {
		return PROPERTIES;
	}
	
	public Object getAdapter(Object element, Class key) {
		
		IJavaElement java= (IJavaElement) element;
		
		if (IPropertySource.class.equals(key)) {
			return getProperties(java);
		} else if (IResource.class.equals(key)) {
			return getResource(java);
		} else if (ISearchPageScoreComputer.class.equals(key)) {
			return fSearchPageScoreComputer;
		} else if (IWorkbenchAdapter.class.equals(key)) {
			return fgJavaWorkbenchAdapter;
		} else if (IResourceLocator.class.equals(key)) {
			return fgResourceLocator;
		}
		
		return null;
	}
	
	private IResource getResource(IJavaElement element) {
		try {
			return element.getCorrespondingResource();
		} catch (JavaModelException e) {
			return null;	
		}
	}
		
	private IPropertySource getProperties(IJavaElement element) {
		return new JavaElementProperties(element);
	}
}