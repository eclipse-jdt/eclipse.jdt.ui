/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Mickael Istria (Red Hat Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.wizardapi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.junit.Assert;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ui.internal.wizards.datatransfer.ILeveledImportStructureProvider;
import org.eclipse.ui.internal.wizards.datatransfer.SmartImportJob;
import org.eclipse.ui.internal.wizards.datatransfer.ZipLeveledStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ProjectConfigurator;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

public class ImporterTest{

	@Test
	public void testImportRawJavaProject() throws Exception {
		IProject project = null;
		try {
			File sourceZip = new File(FileLocator.toFileURL(getClass().getClassLoader().getResource(JavaProjectHelper.JUNIT_SRC_381.toString())).getFile());
			File expandedZip = Files.createTempDirectory("ImporterTest-testImportRawJavaProject").toFile();
			unzip(sourceZip, expandedZip);
			SmartImportJob job = new SmartImportJob(expandedZip, Collections.EMPTY_SET, true, true);
			Map<File, List<ProjectConfigurator>> importProposals = job.getImportProposals(new NullProgressMonitor());
			Assert.assertEquals("No folder should be immediatly detected as project", 0, importProposals.size());
			job.run(new NullProgressMonitor());
			project = ResourcesPlugin.getWorkspace().getRoot().getProject(expandedZip.getName());
			Assert.assertTrue("Project wasn't created", project.exists());
			Assert.assertTrue("Project doesn't have Java nature", project.hasNature(JavaCore.NATURE_ID));
			IJavaProject javaProject = (IJavaProject) project.getNature(JavaCore.NATURE_ID);
			project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
			Assert.assertNotNull("Couldn't resolve type from Java project", javaProject.findType("junit.framework.TestCase"));
			Assert.assertNotNull("Couldn't resolve JRE type from Java project", javaProject.findType("java.util.List"));
		} finally {
			if (project != null) {
				JavaProjectHelper.delete(project);
			}
		}
	}

	private void unzip(File source, File target) throws ZipException, IOException {
		try (ZipFile zip = new ZipFile(source)) {
			ILeveledImportStructureProvider importStructureProvider = new ZipLeveledStructureProvider(zip);
			LinkedList<Object> toProcess = new LinkedList<>();
			toProcess.add(importStructureProvider.getRoot());
			while (!toProcess.isEmpty()) {
				Object current = toProcess.pop();
				String path = importStructureProvider.getFullPath(current);
				File toCreate = null;
				if ("/".equals(path)) { //$NON-NLS-1$
					toCreate = target;
				} else {
					toCreate = new File(target, path);
				}
				if (importStructureProvider.isFolder(current)) {
					toCreate.mkdirs();
				} else {
					try (InputStream content = importStructureProvider.getContents(current)) {
						// known IImportStructureProviders already log an
						// exception before returning null
						if (content != null) {
							Files.copy(content, toCreate.toPath());
						}
					}
				}
				List<?> children = importStructureProvider.getChildren(current);
				if (children != null) {
					toProcess.addAll(children);
				}
			}
		}
	}
}
