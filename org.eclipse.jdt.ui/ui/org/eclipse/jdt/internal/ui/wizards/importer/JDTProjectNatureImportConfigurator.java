/*******************************************************************************
 * Copyright (c) 2014-2016 Red Hat Inc., and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Mickael Istria (Red Hat Inc.) - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards.importer;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;

import org.eclipse.ui.wizards.datatransfer.ProjectConfigurator;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class JDTProjectNatureImportConfigurator implements ProjectConfigurator {

	private static final String BIN= "bin"; //$NON-NLS-1$
	private static final String CLASSPATH= ".classpath"; //$NON-NLS-1$

	@Override
	public boolean canConfigure(IProject project, Set<IPath> ignoredDirectories, IProgressMonitor monitor) {
		return shouldBeAnEclipseProject(project, monitor);
	}

	@Override
	public void configure(IProject project, Set<IPath> ignoredDirectories, IProgressMonitor monitor) {
		try {
			IProjectDescription description = project.getDescription();
			List<String> natures = Arrays.asList(description.getNatureIds());
			if (!natures.contains(JavaCore.NATURE_ID)) {
				List<String> newNatures = new ArrayList<>(natures);
				newNatures.add(JavaCore.NATURE_ID);
				description.setNatureIds(newNatures.toArray(new String[newNatures.size()]));
				project.setDescription(description, monitor);
			}
		} catch (Exception ex) {
			JavaPlugin.log(new Status(
					IStatus.ERROR,
					JavaCore.PLUGIN_ID,
					ex.getMessage(),
					ex));
		}
	}

	@Override
	public boolean shouldBeAnEclipseProject(IContainer container, IProgressMonitor monitor) {
		return container.getFile(new Path(CLASSPATH)).exists();
	}

	@Override
	public Set<IFolder> getFoldersToIgnore(IProject project, IProgressMonitor monitor) {
		Set<IFolder> res = new HashSet<>();
		try {
			IJavaProject javaProject = (IJavaProject)project.getNature(JavaCore.NATURE_ID);
			if (javaProject == null) { // project already has .classpath and .project but Java nature isn't set
				return Collections.emptySet();
			}
			IResource resource = project.getWorkspace().getRoot().findMember(javaProject.getOutputLocation());
			if (resource != null && resource.exists() && resource.getType() == IResource.FOLDER) {
				res.add((IFolder)resource);
			}
			for (IClasspathEntry entry : javaProject.getRawClasspath()) {
				if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
					IFolder sourceFolder = project.getFolder(entry.getPath());
					res.add(sourceFolder);
				}
			}
		} catch (CoreException ex) {
			JavaPlugin.log(new Status(
					IStatus.ERROR,
					JavaCore.PLUGIN_ID,
					ex.getMessage(),
					ex));
		}
		return res;
	}

	@Override
	public Set<File> findConfigurableLocations(File root, IProgressMonitor monitor) {
		// Might be relevant to search .classpath files but those usually come with .project
		// so they're already detected as Eclipse projects
		return null;
	}

	/**
	 * Remove bin/ folder from proposals
	 */
	@Override
	public void removeDirtyDirectories(Map<File, List<ProjectConfigurator>> proposals) {
		Set<File> toRemove = new HashSet<>();
		for (File directory : proposals.keySet()) {
			String path = directory.getAbsolutePath();
			if (!path.endsWith(File.separator)) {
				path += File.separator;
			}
			int index = path.indexOf(File.separator + BIN + File.separator);
			if (index >= 0) {
				File potentialClasspathFile = new File(path.substring(0, index), CLASSPATH);
				if (potentialClasspathFile.isFile()) {
					toRemove.add(directory);
				}
			}
		}
		for (File directory : toRemove) {
			proposals.remove(directory);
		}
	}

}
