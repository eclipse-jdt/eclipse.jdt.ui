/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui;


import org.eclipse.core.resources.IProject;import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterFactory;

import org.eclipse.ui.IPersistableElement;import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.views.properties.IPropertySource;

import org.eclipse.search.ui.ISearchPageScoreComputer;

import org.eclipse.jdt.core.ICompilationUnit;import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IWorkingCopy;import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.search.JavaSearchPageScoreComputer;import org.eclipse.jdt.internal.ui.util.JavaModelUtil;

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
		IResourceLocator.class,
		IPersistableElement.class,
		IProject.class
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
		} else if (IProject.class.equals(key)) {
			return getProject(java);
		} else if (ISearchPageScoreComputer.class.equals(key)) {
			return fSearchPageScoreComputer;
		} else if (IWorkbenchAdapter.class.equals(key)) {
			return fgJavaWorkbenchAdapter;
		} else if (IResourceLocator.class.equals(key)) {
			return fgResourceLocator;
		} else if (IPersistableElement.class.equals(key)) 
			return new PersistableJavaElementFactory(java);
		return null; 
	}
	
	private IResource getResource(IJavaElement element) {
		try {
			IResource r= element.getCorrespondingResource();
			if (r != null)
				return r;
			//1GE8SR2: ITPUI:WIN98 - Task List does not show problems for selection in Hierarchy View
			// check whether the resource is inside a CU. If yes
			// return the CU as the resource
			ICompilationUnit cu= (ICompilationUnit)JavaModelUtil.findParentOfKind(element, IJavaElement.COMPILATION_UNIT);
			if (cu != null) {
				if (cu.isWorkingCopy())
					return cu.getOriginalElement().getUnderlyingResource();
				return cu.getUnderlyingResource();
			}
			return null;
		} catch (JavaModelException e) {
			return null;	
		}
	}
		
	private IResource getProject(IJavaElement element) {
		return element.getJavaProject().getProject();
	}

	private IPropertySource getProperties(IJavaElement element) {
		return new JavaElementProperties(element);
	}
}