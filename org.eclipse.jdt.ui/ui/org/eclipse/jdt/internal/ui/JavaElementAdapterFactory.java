/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui;


import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterFactory;

import org.eclipse.ui.IContributorResourceAdapter;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.views.properties.FilePropertySource;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.ResourcePropertySource;
import org.eclipse.ui.views.tasklist.ITaskListResourceAdapter;

import org.eclipse.search.ui.ISearchPageScoreComputer;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.IWorkingCopy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.search.JavaSearchPageScoreComputer;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

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
		IProject.class,
		IContributorResourceAdapter.class,
		ITaskListResourceAdapter.class
	};
	
	private ISearchPageScoreComputer fSearchPageScoreComputer= new JavaSearchPageScoreComputer();
	private static IResourceLocator fgResourceLocator= new ResourceLocator();
	private static JavaWorkbenchAdapter fgJavaWorkbenchAdapter= new JavaWorkbenchAdapter();
	private static IContributorResourceAdapter fgContributorResourceAdapter= new JavaContributorResourceAdapter();
	private static ITaskListResourceAdapter fgTaskListAdapter= new JavaTaskListAdapter();
	
	public Class[] getAdapterList() {
		return PROPERTIES;
	}
	
	public Object getAdapter(Object element, Class key) {
		
		IJavaElement java= (IJavaElement) element;
		
		if (IPropertySource.class.equals(key)) {
			return getProperties(java);
		} if (IResource.class.equals(key)) {
			return getResource(java);
		} if (IProject.class.equals(key)) {
			return getProject(java);
		} if (ISearchPageScoreComputer.class.equals(key)) {
			return fSearchPageScoreComputer;
		} if (IWorkbenchAdapter.class.equals(key)) {
			return fgJavaWorkbenchAdapter;
		} if (IResourceLocator.class.equals(key)) {
			return fgResourceLocator;
		} if (IPersistableElement.class.equals(key)) {
			return new PersistableJavaElementFactory(java);
		} if (IContributorResourceAdapter.class.equals(key)) {
			return fgContributorResourceAdapter;
		} if (ITaskListResourceAdapter.class.equals(key)) {
			return fgTaskListAdapter;
		}
		return null; 
	}
	
	private IResource getResource(IJavaElement element) {
		/*
		 * Map a type to the corresponding CU.
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
			return element.getCorrespondingResource();
		} catch (JavaModelException e) {
			return null;	
		}
	}
	
	private IResource getProject(IJavaElement element) {
		return element.getJavaProject().getProject();
	}

	private IPropertySource getProperties(IJavaElement element) {
		IResource resource= getResource(element);
		if (resource == null)
			return new JavaElementProperties(element);
		if (resource.getType() == IResource.FILE)
			return new FilePropertySource((IFile) resource);
		return new ResourcePropertySource((IResource) resource);
	}
}