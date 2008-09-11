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
package org.eclipse.jdt.internal.ui.exampleprojects;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.zip.ZipFile;

import org.osgi.framework.Bundle;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;

import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;
import org.eclipse.ui.wizards.datatransfer.ZipFileStructureProvider;

public class ExampleProjectCreationOperation implements IRunnableWithProgress {

	private IResource fElementToOpen;

	private ExampleProjectCreationWizardPage fPage;
	private IOverwriteQuery fOverwriteQuery;

	/*
	 * Constructor for ExampleProjectCreationOperation
	 */
	public ExampleProjectCreationOperation(ExampleProjectCreationWizardPage page, IOverwriteQuery overwriteQuery) {
		fElementToOpen= null;
		fPage= page;
		fOverwriteQuery= overwriteQuery;
	}

	/*
	 * @see IRunnableWithProgress#run(IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		if (monitor == null) {
			monitor= new NullProgressMonitor();
		}
		try {
			createProject(fPage, monitor);
		} finally {
			monitor.done();
		}
	}

	public IResource getElementToOpen() {
		return fElementToOpen;
	}


	private void createProject(ExampleProjectCreationWizardPage page, IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		IWorkspaceRoot root= ExampleProjectsPlugin.getWorkspace().getRoot();
		IConfigurationElement desc= page.getConfigurationElement();

		String encoding= desc.getAttribute("encoding"); //$NON-NLS-1$

		IConfigurationElement[] imports= desc.getChildren("import"); //$NON-NLS-1$
		IConfigurationElement[] natures= desc.getChildren("nature"); //$NON-NLS-1$
		IConfigurationElement[] references= desc.getChildren("references"); //$NON-NLS-1$
		int nImports= (imports == null) ? 0 : imports.length;
		int nNatures= (natures == null) ? 0 : natures.length;
		int nReferences= (references == null) ? 0 : references.length;

		monitor.beginTask(ExampleProjectMessages.ExampleProjectCreationOperation_op_desc_proj, nImports + 2);

		String name= page.getName();

		String[] natureIds= new String[nNatures];
		for (int i= 0; i < nNatures; i++) {
			natureIds[i]= natures[i].getAttribute("id"); //$NON-NLS-1$
		}
		IProject[] referencedProjects= new IProject[nReferences];
		for (int i= 0; i < nReferences; i++) {
			referencedProjects[i]= root.getProject(references[i].getAttribute("id")); //$NON-NLS-1$
		}

		IProject proj= configNewProject(root, name, natureIds, referencedProjects, encoding, monitor);

		for (int i= 0; i < nImports; i++) {
			doImports(proj, imports[i], new SubProgressMonitor(monitor, 1));
		}

		String open= desc.getAttribute("open"); //$NON-NLS-1$
		if (open != null && open.length() > 0) {
			IResource fileToOpen= proj.findMember(new Path(open));
			if (fileToOpen != null) {
				fElementToOpen= fileToOpen;
			}
		}

	}

	private IProject configNewProject(IWorkspaceRoot root, String name, String[] natureIds, IProject[] referencedProjects, String encoding, IProgressMonitor monitor) throws InvocationTargetException {
		try {
			IProject project= root.getProject(name);
			if (!project.exists()) {
				project.create(null);
			}
			if (!project.isOpen()) {
				project.open(null);
			}
			IProjectDescription desc= project.getDescription();
			desc.setLocation(null);
			desc.setNatureIds(natureIds);
			desc.setReferencedProjects(referencedProjects);

			project.setDescription(desc, new SubProgressMonitor(monitor, 1));

			if (encoding != null) {
				project.setDefaultCharset(encoding, new SubProgressMonitor(monitor, 1));
			}

			return project;
		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		}
	}

	private void doImports(IProject project, IConfigurationElement curr, IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		try {
			IPath destPath;
			String name= curr.getAttribute("dest"); //$NON-NLS-1$
			if (name == null || name.length() == 0) {
				destPath= project.getFullPath();
			} else {
				IFolder folder= project.getFolder(name);
				if (!folder.exists()) {
					folder.create(true, true, null);
				}
				destPath= folder.getFullPath();
			}
			String importPath= curr.getAttribute("src"); //$NON-NLS-1$
			if (importPath == null) {
				importPath= ""; //$NON-NLS-1$
				ExampleProjectsPlugin.log("projectsetup descriptor: import missing"); //$NON-NLS-1$
				return;
			}

			ZipFile zipFile= getZipFileFromPluginDir(importPath, getContributingPlugin(curr));
			importFilesFromZip(zipFile, destPath, new SubProgressMonitor(monitor, 1));
		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		}
	}

	private Bundle getContributingPlugin(IConfigurationElement configurationElement) {
		String namespace= configurationElement.getContributor().getName();
		return Platform.getBundle(namespace);
	}

	private ZipFile getZipFileFromPluginDir(String pluginRelativePath, Bundle pluginDescriptor) throws CoreException {
		try {
			URL starterURL= pluginDescriptor.getEntry(pluginRelativePath);
			return new ZipFile(FileLocator.toFileURL(starterURL).getFile());
		} catch (IOException e) {
			String message= pluginRelativePath + ": " + e.getMessage(); //$NON-NLS-1$
			Status status= new Status(IStatus.ERROR, ExampleProjectsPlugin.getPluginId(), IStatus.ERROR, message, e);
			throw new CoreException(status);
		}
	}

	private void importFilesFromZip(ZipFile srcZipFile, IPath destPath, IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		ZipFileStructureProvider structureProvider=	new ZipFileStructureProvider(srcZipFile);
		ImportOperation op= new ImportOperation(destPath, structureProvider.getRoot(), structureProvider, fOverwriteQuery);
		op.run(monitor);
	}
}
