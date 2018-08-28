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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;

import org.eclipse.ui.wizards.datatransfer.ProjectConfigurator;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.wizards.ClassPathDetector;

public class ProjectWithJavaResourcesImportConfigurator implements ProjectConfigurator {

	private static final String CLASSPATH= ".classpath"; //$NON-NLS-1$
	private static final String BIN= "bin"; //$NON-NLS-1$

	private final static class JavaResourceExistsFinder implements IResourceVisitor {
		private boolean hasJavaFile;
		private Set<IPath> ignoredDirectories;

		public JavaResourceExistsFinder(Set<IPath> ignoredDirectories) {
			this.ignoredDirectories = ignoredDirectories;
		}

		@Override
		public boolean visit(final IResource resource) throws CoreException {
			if (this.ignoredDirectories != null) {
				for (IPath ignoredDirectory : this.ignoredDirectories) {
					if (ignoredDirectory.isPrefixOf(resource.getLocation())) {
						return false;
					}
				}
			}

			this.hasJavaFile = this.hasJavaFile || (resource.getType() == IResource.FILE && resource.getName().endsWith(".java")); //$NON-NLS-1$
			return !this.hasJavaFile;
		}

		public boolean hasJavaFile() {
			return this.hasJavaFile;
		}

	}

	@Override
	public boolean canConfigure(IProject project, Set<IPath> ignoredDirectories, IProgressMonitor monitor) {
		JavaResourceExistsFinder javaResourceFinder = new JavaResourceExistsFinder(ignoredDirectories);
		try {
			project.accept(javaResourceFinder);
		} catch (CoreException ex) {
			JavaPlugin.log(new Status(
					IStatus.ERROR,
					JavaCore.PLUGIN_ID,
					ex.getMessage(),
					ex));
			return false;
		}
		return javaResourceFinder.hasJavaFile();
	}

	@Override
	public void configure(IProject project, Set<IPath> ignoredDirectories, IProgressMonitor monitor) {
		try {
			SubMonitor subMonitor = SubMonitor.convert(monitor, 6);
			IProjectDescription description = project.getDescription();
			List<String> natures = Arrays.asList(description.getNatureIds());
			IJavaProject javaNature = null;
			if (!natures.contains(JavaCore.NATURE_ID)) {
				List<String> newNatures = new ArrayList<>(natures);
				newNatures.add(JavaCore.NATURE_ID);
				description.setNatureIds(newNatures.toArray(new String[newNatures.size()]));
				project.setDescription(description, subMonitor.split(1));
				javaNature = JavaCore.create(project);
				javaNature.open(subMonitor.split(1));
			} else {
				javaNature = (IJavaProject)project.getNature(JavaCore.NATURE_ID);
				subMonitor.worked(2);
			}
			if (!project.getFile(CLASSPATH).exists()) {
				final ClassPathDetector detector= new ClassPathDetector(project, subMonitor.split(2));
				IClasspathEntry[] entries= detector.getClasspath();
				IPath outputLocation= detector.getOutputLocation();
				if (entries.length == 0) {
					entries = PreferenceConstants.getDefaultJRELibrary();
				}
				javaNature.setRawClasspath(entries, subMonitor.split(1));
				if (outputLocation == null) {
					IFolder binFolder = project.getFolder(BIN);
					if (!binFolder.exists()) {
						binFolder.create(false, true, subMonitor.split(1));
					} else {
						subMonitor.worked(1);
					}
					outputLocation = binFolder.getFullPath();
				} else {
					subMonitor.worked(1);
				}
				javaNature.setOutputLocation(outputLocation, subMonitor.split(1));
			} else {
				subMonitor.worked(4);
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
		return false; // Having a .java file isn't enough to guarantee we are at the root of a project
	}

	@Override
	public Set<IFolder> getFoldersToIgnore(IProject project, IProgressMonitor monitor) {
		Set<IFolder> res = new HashSet<>();
		try {
			IJavaProject javaProject = (IJavaProject)project.getNature(JavaCore.NATURE_ID);
			if (javaProject == null) {
				return res;
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
		// No way to immediately deduce project directories from Java file
		return null;
	}

}
