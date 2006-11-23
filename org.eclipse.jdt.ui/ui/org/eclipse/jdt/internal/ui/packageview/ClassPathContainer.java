/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.packageview;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.model.IWorkbenchAdapter;

import org.eclipse.ui.ide.IDE;

import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * Representation of class path containers in Java UI.
 */
public class ClassPathContainer extends PackageFragmentRootContainer {

	private IClasspathEntry fClassPathEntry;
	private IClasspathContainer fContainer;

	public static class RequiredProjectWrapper implements IAdaptable, IWorkbenchAdapter {

		private final IJavaElement fProject;
		private static ImageDescriptor DESC_OBJ_PROJECT;	
		{
			ISharedImages images= JavaPlugin.getDefault().getWorkbench().getSharedImages(); 
			DESC_OBJ_PROJECT= images.getImageDescriptor(IDE.SharedImages.IMG_OBJ_PROJECT);
		}

		public RequiredProjectWrapper(IJavaElement project) {
			this.fProject= project;
		}
		
		public IJavaElement getProject() {
			return fProject; 
		}
		
		public Object getAdapter(Class adapter) {
			if (adapter == IWorkbenchAdapter.class) 
				return this;
			return null;
		}

		public Object[] getChildren(Object o) {
			return null;
		}

		public ImageDescriptor getImageDescriptor(Object object) {
			return DESC_OBJ_PROJECT;
		}

		public String getLabel(Object o) {
			return fProject.getElementName();
		}

		public Object getParent(Object o) {
			return null;
		}
	}

	public ClassPathContainer(IJavaProject parent, IClasspathEntry entry) {
		super(parent);
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
			if (getJavaProject().equals(other.getJavaProject()) &&
				fClassPathEntry.equals(other.fClassPathEntry)) {
				return true;	
			}
			
		}
		return false;
	}

	public int hashCode() {
		return getJavaProject().hashCode()*17+fClassPathEntry.hashCode();
	}

	public IPackageFragmentRoot[] getPackageFragmentRoots() {
		return getJavaProject().findPackageFragmentRoots(fClassPathEntry);
	}

	public Object[] getChildren() {
		return concatenate(getPackageFragmentRoots(), getRequiredProjects());
	}

	private Object[] getRequiredProjects() {
		List list= new ArrayList();
		if (fContainer != null) {
			IClasspathEntry[] classpathEntries= fContainer.getClasspathEntries();
			if (classpathEntries == null) {
				// invalid implementation of a classpath container
				JavaPlugin.log(new IllegalArgumentException("Invalid classpath container implementation: getClasspathEntries() returns null. " + fContainer.getPath())); //$NON-NLS-1$
				return new Object[0];
			}
			
			IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
			for (int i= 0; i < classpathEntries.length; i++) {
				IClasspathEntry entry= classpathEntries[i];
				if (entry.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
					IResource resource= root.findMember(entry.getPath());
					if (resource instanceof IProject)
						list.add(new RequiredProjectWrapper(JavaCore.create(resource)));
				}
			}
		}
		return list.toArray();
	}

	private static Object[] concatenate(Object[] a1, Object[] a2) {
		int a1Len= a1.length;
		int a2Len= a2.length;
		Object[] res= new Object[a1Len + a2Len];
		System.arraycopy(a1, 0, res, 0, a1Len);
		System.arraycopy(a2, 0, res, a1Len, a2Len); 
		return res;
	}

	public ImageDescriptor getImageDescriptor() {
		return JavaPluginImages.DESC_OBJS_LIBRARY;
	}

	public String getLabel() {
		if (fContainer != null)
			return fContainer.getDescription();
		
		IPath path= fClassPathEntry.getPath();
		String containerId= path.segment(0);
		ClasspathContainerInitializer initializer= JavaCore.getClasspathContainerInitializer(containerId);
		if (initializer != null) {
			String description= initializer.getDescription(path, getJavaProject());
			return Messages.format(PackagesMessages.ClassPathContainer_unbound_label, description); 
		}
		return Messages.format(PackagesMessages.ClassPathContainer_unknown_label, path.toString()); 
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
