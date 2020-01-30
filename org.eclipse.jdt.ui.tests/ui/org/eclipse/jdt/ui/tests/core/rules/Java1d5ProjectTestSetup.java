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
 *     Fabrice TIERCELIN - Split the tests
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core.rules;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

/**
 * Setups the tests related to Java 5.
 */
public class Java1d5ProjectTestSetup extends ProjectTestSetup {
	public static final String PROJECT_NAME1d5= "TestSetupProject1d5";

	public static IJavaProject getProject() {
		IProject project= ResourcesPlugin.getWorkspace().getRoot().getProject(PROJECT_NAME1d5);
		return JavaCore.create(project);
	}

	public static IClasspathEntry[] getDefaultClasspath() throws CoreException {
		IPath[] rtJarPath= JavaProjectHelper.findRtJar(JavaProjectHelper.RT_STUBS_15);
		return new IClasspathEntry[] { JavaCore.newLibraryEntry(rtJarPath[0], rtJarPath[1], rtJarPath[2], true) };
	}

	@Override
	protected boolean projectExists() {
		return getProject().exists();
	}

	@Override
	protected IJavaProject createAndInitializeProject() throws CoreException {
		IJavaProject javaProject= JavaProjectHelper.createJavaProject(PROJECT_NAME1d5, "bin");
		javaProject.setRawClasspath(getDefaultClasspath(), null);
		JavaProjectHelper.set15CompilerOptions(javaProject);
		return javaProject;
	}
}
