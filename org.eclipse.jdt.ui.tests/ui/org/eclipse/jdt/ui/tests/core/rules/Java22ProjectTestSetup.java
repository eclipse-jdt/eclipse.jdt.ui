/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
 *     Red Hat Inc. - copied and modified from Java17ProjectTestSetup
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core.rules;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.core.ClasspathEntry;

public class Java22ProjectTestSetup extends ProjectTestSetup {

	public static final String PROJECT_NAME22= "TestSetupProject22";

	private final String projectName;

	@Override
	public IJavaProject getProject() {
		IProject project= ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		return JavaCore.create(project);
	}

	@Override
	public IClasspathEntry[] getDefaultClasspath() throws CoreException {
		IPath[] rtJarPath= JavaProjectHelper.findRtJar(JavaProjectHelper.RT_STUBS22);
		IClasspathAttribute[] extraAttributes= { JavaCore.newClasspathAttribute(IClasspathAttribute.MODULE, Boolean.TRUE.toString()) };
		return new IClasspathEntry[] { JavaCore.newLibraryEntry(rtJarPath[0], rtJarPath[1], rtJarPath[2], ClasspathEntry.NO_ACCESS_RULES, extraAttributes, true) };
	}

	public Java22ProjectTestSetup() {
		projectName= PROJECT_NAME22;
	}

	public Java22ProjectTestSetup( String projectName) {
		this.projectName= projectName;
	}

	@Override
	protected boolean projectExists() {
		return getProject().exists();
	}

	@Override
	protected IJavaProject createAndInitializeProject() throws CoreException {
		IJavaProject javaProject= JavaProjectHelper.createJavaProject(projectName, "bin");
		javaProject.setRawClasspath(getDefaultClasspath(), null);
		JavaProjectHelper.set22CompilerOptions(javaProject);
		return javaProject;
	}

}
