/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.util;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IOpenable;

public class ResourcesUtility {

	private ResourcesUtility() {
	}

	public static void createDerivedFolder(IFolder folder, boolean force, boolean local, IProgressMonitor monitor) throws CoreException {
		if (!folder.exists()) {
			IContainer parent= folder.getParent();
			if (parent instanceof IFolder) {
				createDerivedFolder((IFolder)parent, force, local, null);
			}
			folder.create(force ? (IResource.FORCE | IResource.DERIVED) : IResource.DERIVED, local, monitor);
		}
	}

	/**
	 * Creates a folder and all parent folders if not existing.
	 * Project must exist.
	 * <code> org.eclipse.ui.dialogs.ContainerGenerator</code> is too heavy
	 * (creates a runnable)
	 * @param folder the folder to create
	 * @param force a flag controlling how to deal with resources that
	 *    are not in sync with the local file system
	 * @param local a flag controlling whether or not the folder will be local
	 *    after the creation
	 * @param monitor the progress monitor
	 * @throws CoreException thrown if the creation failed
	 */
	public static void createFolder(IFolder folder, boolean force, boolean local, IProgressMonitor monitor) throws CoreException {
		if (!folder.exists()) {
			IContainer parent= folder.getParent();
			if (parent instanceof IFolder) {
				createFolder((IFolder)parent, force, local, null);
			}
			folder.create(force, local, monitor);
		}
	}


	public static IFile[] getFiles(ICompilationUnit[] cus) {
		List<IResource> files= new ArrayList<>(cus.length);
		for (ICompilationUnit cu : cus) {
			IResource resource= cu.getResource();
			if (resource != null && resource.getType() == IResource.FILE)
				files.add(resource);
		}
		return files.toArray(new IFile[files.size()]);
	}

	public static IFile getFile(ICompilationUnit cu) {
		IResource resource= cu.getResource();
		if (resource != null && resource.getType() == IResource.FILE)
			return (IFile)resource;
		else
			return null;
	}

	//----- other ------------------------------

	public static IResource getResource(Object o){
		if (o instanceof IResource)
			return (IResource)o;
		if (o instanceof IJavaElement)
			return getResource((IJavaElement)o);
		return null;
	}

	public static IResource getResource(IJavaElement element){
		if (element.getElementType() == IJavaElement.COMPILATION_UNIT)
			return ((ICompilationUnit) element).getResource();
		else if (element instanceof IOpenable)
			return element.getResource();
		else
			return null;
	}



	public static void createProject(IProject project, URI locationURI, IProgressMonitor monitor) throws CoreException {
		if (monitor == null) {
			monitor= new NullProgressMonitor();
		}
		monitor.beginTask(ManipulationsCommonUtilMessages.BuildPathsBlock_operationdesc_project, 10);

		// create the project
		try {
			if (!project.exists()) {
				IProjectDescription desc= project.getWorkspace().newProjectDescription(project.getName());
				if (locationURI != null && ResourcesPlugin.getWorkspace().getRoot().getLocationURI().equals(locationURI)) {
					locationURI= null;
				}
				desc.setLocationURI(locationURI);
				project.create(desc, monitor);
				monitor= null;
			}
			if (!project.isOpen()) {
				project.open(monitor);
				monitor= null;
			}
		} finally {
			if (monitor != null) {
				monitor.done();
			}
		}
	}
}
