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
 * derived from corresponding file in org.eclipse.jdt.junit.tests
 * instead extending TestSetup for junit4 ExternalResource is extended
 * to allow use as junit "@Rule"
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core.rules;

import org.junit.rules.ExternalResource;

import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IFolder;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;


public class JUnitWorkspaceTestSetup extends ExternalResource {

	public static final String WORKSPACE_PATH= "testresources/JUnitWorkspace/";

	private static final String PROJECT_NAME_3= "JUnitTests";
	private static final String PROJECT_NAME_4= "JUnit4Tests";
	private static final String SRC_NAME= "src";

	boolean fJUnit4;
	private static IJavaProject fgProject;
	private static IPackageFragmentRoot fgRoot;

	public JUnitWorkspaceTestSetup(boolean jUnit4) {
		fJUnit4= jUnit4;
	}

	public static IJavaProject getJavaProject() {
		return fgProject;
	}

	public static IPackageFragmentRoot getRoot() {
		return fgRoot;
	}

	public static String getProjectPath() {
		return WORKSPACE_PATH + fgProject.getElementName() + '/';
	}

	@Override
	protected void before() throws Throwable {
		if (fJUnit4) {
			fgProject= JavaProjectHelper.createJavaProject(PROJECT_NAME_4, "bin");
			JavaProjectHelper.addRTJar(fgProject);
			IClasspathEntry cpe= JavaCore.newContainerEntry(JUnitCore.JUNIT4_CONTAINER_PATH);
			JavaProjectHelper.addToClasspath(fgProject, cpe);

		} else {
			fgProject= JavaProjectHelper.createJavaProject(PROJECT_NAME_3, "bin");
			JavaProjectHelper.addRTJar13(fgProject);
			IClasspathEntry cpe= JavaCore.newContainerEntry(JUnitCore.JUNIT3_CONTAINER_PATH);
			JavaProjectHelper.addToClasspath(fgProject, cpe);
		}
		fgRoot= JavaProjectHelper.addSourceContainer(fgProject, SRC_NAME);
		JavaProjectHelper.importResources((IFolder) fgRoot.getResource(), JavaTestPlugin.getDefault().getBundle(), getProjectPath() + SRC_NAME);
	}

	@Override
	protected void after() {
		try {
			JavaProjectHelper.delete(fgProject);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		fgProject= null;
		fgRoot= null;
	}

}
