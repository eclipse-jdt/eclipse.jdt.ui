/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.performance;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import junit.framework.Assert;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;


public class SWTTestProject {

	public static final String PROJECT= "org.eclipse.swt";
	private static final String PROJECT_ZIP= "/testresources/org.eclipse.swt-R3_0.zip";

	private IJavaProject fProject;

	public SWTTestProject() throws Exception {
		setUpProject();
	}

	public IJavaProject getProject() {
		return fProject;
	}

	public void delete() throws Exception {
		if (fProject != null && fProject.exists()) {
			JavaProjectHelper.delete(fProject);
		}
	}

	private void setUpProject() throws IOException, ZipException, CoreException {
		String workspacePath= ResourcesPlugin.getWorkspace().getRoot().getLocation().toString() + "/";
		FileTool.unzip(new ZipFile(FileTool.getFileInPlugin(JavaTestPlugin.getDefault(), new Path(PROJECT_ZIP))), new File(workspacePath));
		File oldFile= new File(workspacePath + PROJECT + "/.classpath_win32");
		File newFile= new File(workspacePath + PROJECT + "/.classpath");
		Assert.assertTrue(oldFile.renameTo(newFile));

		IProject project= createExistingProject(PROJECT);
		fProject= JavaCore.create(project);
		
		//some classes in the archive are not 1.4 compliant, e.g. GridData uses 'enum' as identifier
		Map options= fProject.getOptions(false);
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_3, options);
		fProject.setOptions(options);
		
		Assert.assertTrue(fProject.exists());
	}

	private IProject createExistingProject(String projectName) throws CoreException {
		IWorkspace workspace= ResourcesPlugin.getWorkspace();
		IProject project= workspace.getRoot().getProject(projectName);
		IProjectDescription description= workspace.newProjectDescription(projectName);
		description.setLocation(null);

		project.create(description, null);
		project.open(null);
		return project;
	}
}
