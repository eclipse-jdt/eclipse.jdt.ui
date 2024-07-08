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
 *     Red Hat Inc - Moved logic from various UI classes
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.util;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

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

	/*
	 * Moved from org.eclipse.jdt.internal.ui.util.CoreUtility
	 */
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
	 *
	 * Moved from org.eclipse.jdt.internal.ui.util.CoreUtility
	 *
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


	/*
	 * Moved from org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil
	 */
	public static IFile[] getFiles(ICompilationUnit[] cus) {
		List<IResource> files= new ArrayList<>(cus.length);
		for (ICompilationUnit cu : cus) {
			IResource resource= cu.getResource();
			if (resource != null && resource.getType() == IResource.FILE)
				files.add(resource);
		}
		return files.toArray(new IFile[files.size()]);
	}

	/*
	 * Moved from org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil
	 */
	public static IFile getFile(ICompilationUnit cu) {
		IResource resource= cu.getResource();
		if (resource != null && resource.getType() == IResource.FILE)
			return (IFile)resource;
		else
			return null;
	}

	/*
	 * Moved from org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil
	 */

	public static IResource getResource(Object o){
		if (o instanceof IResource)
			return (IResource)o;
		if (o instanceof IJavaElement)
			return getResource((IJavaElement)o);
		return null;
	}

	/*
	 * Moved from org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil
	 */
	public static IResource getResource(IJavaElement element){
		if (element.getElementType() == IJavaElement.COMPILATION_UNIT)
			return element.getResource();
		else if (element instanceof IOpenable)
			return element.getResource();
		else
			return null;
	}

	/*
	 * Adapted / moved from  org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathsBlock
	 */
	public static void createProject(IProject project, URI locationURI, IProgressMonitor monitor) throws CoreException {
		SubMonitor subMonitor= SubMonitor.convert(monitor, ManipulationsCommonUtilMessages.BuildPathsBlock_operationdesc_project, 2);
		try {
			if (!project.exists()) {
				IProjectDescription desc= project.getWorkspace().newProjectDescription(project.getName());
				if (locationURI != null && ResourcesPlugin.getWorkspace().getRoot().getLocationURI().equals(locationURI)) {
					locationURI= null;
				}
				desc.setLocationURI(locationURI);
				project.create(desc, subMonitor.newChild(1));
			}
			if (!project.isOpen()) {
				project.open(subMonitor.newChild(1));
			}
		} finally {
			subMonitor.done();
		}
	}
}
