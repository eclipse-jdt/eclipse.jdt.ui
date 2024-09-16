/*******************************************************************************
 * Copyright (c) 2024 GK Software SE and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stephan Herrmann - Initial API and implementation
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

/** Variant derived from Java17ProjectTestSetup */
public class Java23ProjectTestSetup extends ProjectTestSetup {

	public static final String PROJECT_NAME23= "TestSetupProject23";

	private final String projectName;

	private final boolean enable_preview_feature;

	@Override
	public IJavaProject getProject() {
		IProject project= ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		return JavaCore.create(project);
	}

	@Override
	public IClasspathEntry[] getDefaultClasspath() throws CoreException {
		IPath[] rtJarPath= JavaProjectHelper.findRtJar(JavaProjectHelper.RT_STUBS17);
		IClasspathAttribute[] extraAttributes= { JavaCore.newClasspathAttribute(IClasspathAttribute.MODULE, Boolean.TRUE.toString()) };
		return new IClasspathEntry[] { JavaCore.newLibraryEntry(rtJarPath[0], rtJarPath[1], rtJarPath[2], ClasspathEntry.NO_ACCESS_RULES, extraAttributes, true) };
	}

	public Java23ProjectTestSetup( boolean enable_preview_feature) {
		this.enable_preview_feature= enable_preview_feature;
		projectName= PROJECT_NAME23;
	}

	public Java23ProjectTestSetup( String projectName, boolean enable_preview_feature) {
		this.enable_preview_feature= enable_preview_feature;
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
		JavaProjectHelper.set23CompilerOptions(javaProject, enable_preview_feature);
		return javaProject;
	}

}
