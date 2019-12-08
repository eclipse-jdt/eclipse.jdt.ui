/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * derived from corresponding file in org.eclipse.jdt.ui.tests.core
 * instead extending TestSetup for junit4 ExternalResource is extended
 * to allow use as junit "@Rule"
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core.rules;


import org.junit.rules.ExternalResource;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.CoreUtility;


/**
 * Sets up an 1.5 project with rtstubs15.jar and compiler, code formatting, code generation, and template options.
 */
public class ProjectTestSetup extends ExternalResource {

	public static final String PROJECT_NAME= "TestSetupProject";

	public static IJavaProject getProject() {
		IProject project= ResourcesPlugin.getWorkspace().getRoot().getProject(PROJECT_NAME);
		return JavaCore.create(project);
	}

	public static IClasspathEntry[] getDefaultClasspath() throws CoreException {
		IPath[] rtJarPath= JavaProjectHelper.findRtJar(JavaProjectHelper.RT_STUBS_15);
		return new IClasspathEntry[] {  JavaCore.newLibraryEntry(rtJarPath[0], rtJarPath[1], rtJarPath[2], true) };
	}


	private IJavaProject fJProject;

	private boolean fAutobuilding;

	 @Override
     protected void before() throws Throwable {

		if (projectExists()) { // allow nesting of ProjectTestSetups
			return;
		}

		fAutobuilding = CoreUtility.setAutoBuilding(false);

		fJProject= createAndInitializeProject();

		JavaCore.setOptions(TestOptions.getDefaultOptions());
		TestOptions.initializeCodeGenerationOptions();
		JavaPlugin.getDefault().getCodeTemplateStore().load();
	}

	protected boolean projectExists() {
		return getProject().exists();
	}

	protected IJavaProject createAndInitializeProject() throws CoreException {
		IJavaProject javaProject= JavaProjectHelper.createJavaProject(PROJECT_NAME, "bin");
		javaProject.setRawClasspath(getDefaultClasspath(), null);
		TestOptions.initializeProjectOptions(javaProject);
		return javaProject;
	}

	@Override
    protected void after() {
		if (fJProject != null) {
			try {
				JavaProjectHelper.delete(fJProject);
				CoreUtility.setAutoBuilding(fAutobuilding);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}

}
