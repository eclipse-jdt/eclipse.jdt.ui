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
package org.eclipse.jdt.internal.ui.packageview;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;

import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.ui.model.IWorkbenchAdapter;

import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementImageDescriptor;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;

/**
 * Representation of class path containers in Java UI.
 */
public class ClassPathContainer implements IAdaptable, IWorkbenchAdapter {
	private IJavaProject fProject;
	private IClasspathEntry fClassPathEntry;
	private IClasspathContainer fContainer;


	public ClassPathContainer(IJavaProject parent, IClasspathEntry entry) {
		fProject= parent;
		fClassPathEntry= entry;
		try {
			fContainer= JavaCore.getClasspathContainer(entry.getPath(), parent);
		} catch (JavaModelException e) {
			fContainer= null;
		}
	}

	public boolean equals(Object obj) {
		if (obj instanceof ClassPathContainer) {
			ClassPathContainer other = (ClassPathContainer)obj;
			if (fProject.equals(other.fProject) &&
				fClassPathEntry.equals(other.fClassPathEntry)) {
				return true;	
			}
			
		}
		return false;
	}

	public int hashCode() {
		return fProject.hashCode()*17+fClassPathEntry.hashCode();
	}

	public Object[] getPackageFragmentRoots() {
		return fProject.findPackageFragmentRoots(fClassPathEntry);
	}

	public Object getAdapter(Class adapter) {
		if (adapter == IWorkbenchAdapter.class) 
			return this;
		if ((adapter == IResource.class) && (fContainer instanceof IAdaptable))
			return ((IAdaptable)fContainer).getAdapter(IResource.class);
		return null;
	}

	public Object[] getChildren(Object o) {
		return getPackageFragmentRoots();
	}

	public ImageDescriptor getImageDescriptor(Object object) {
		ImageDescriptor desc= JavaPluginImages.DESC_OBJS_LIBRARY;
		if (fContainer == null) {
			desc = new JavaElementImageDescriptor(desc, JavaElementImageDescriptor.ERROR, JavaElementImageProvider.SMALL_SIZE);
		}
		return desc;
	}

	public String getLabel(Object o) {
		if (fContainer != null)
			return fContainer.getDescription();
		
		IPath path= fClassPathEntry.getPath();
		String containerId= path.segment(0);
		ClasspathContainerInitializer initializer= JavaCore.getClasspathContainerInitializer(containerId);
		if (initializer != null) {
			String description= initializer.getDescription(path, fProject);
			return PackagesMessages.getFormattedString("ClassPathContainer.unbound_label", description); //$NON-NLS-1$
		}
		return PackagesMessages.getFormattedString("ClassPathContainer.unknown_label", path.toString()); //$NON-NLS-1$
	}

	public Object getParent(Object o) {
		return getJavaProject();
	}

	public IJavaProject getJavaProject() {
		return fProject;
	}
	
	public IClasspathEntry getClasspathEntry() {
		return fClassPathEntry;
	}
	
	static boolean contains(IJavaProject project, IClasspathEntry entry, IPackageFragmentRoot root) {
		IPackageFragmentRoot[] roots= project.findPackageFragmentRoots(entry);
		for (int i= 0; i < roots.length; i++) {
			if (roots[i].equals(root))
				return true;
		}
		return false;
	}
}
