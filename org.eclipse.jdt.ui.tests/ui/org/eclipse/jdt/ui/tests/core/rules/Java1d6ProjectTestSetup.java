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

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

/**
 * Setup the tests related to Java 6 (i.e. Mustang).
 */
public class Java1d6ProjectTestSetup extends ProjectTestSetup {
	@Override
	public IClasspathEntry[] getDefaultClasspath() throws CoreException {
		IPath[] rtJarPath= JavaProjectHelper.findRtJar(JavaProjectHelper.RT_STUBS_16);
		return new IClasspathEntry[] { JavaCore.newLibraryEntry(rtJarPath[0], rtJarPath[1], rtJarPath[2], true) };
	}

	@Override
	protected IJavaProject createAndInitializeProject() throws CoreException {
		IJavaProject javaProject= JavaProjectHelper.createJavaProject(PROJECT_NAME, "bin");
		javaProject.setRawClasspath(getDefaultClasspath(), null);
		JavaProjectHelper.set16CompilerOptions(javaProject);
		return javaProject;
	}
}
