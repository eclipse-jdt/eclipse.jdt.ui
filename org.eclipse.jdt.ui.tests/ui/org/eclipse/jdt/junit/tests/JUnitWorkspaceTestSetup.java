/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.junit.tests;


import junit.extensions.TestSetup;
import junit.framework.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;

import org.eclipse.core.resources.IFolder;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.junit.buildpath.JUnitContainerInitializer;

public class JUnitWorkspaceTestSetup extends TestSetup {

	public static final String WORKSPACE_PATH= "testresources/JUnitWorkspace/";

	private static final String PROJECT_NAME_3= "JUnitTests";
	private static final String PROJECT_NAME_4= "JUnit4Tests";
	private static final String SRC_NAME= "src";

	private boolean fJUnit4;
	private static IJavaProject fgProject;
	private static IPackageFragmentRoot fgRoot;

	public JUnitWorkspaceTestSetup(Test test, boolean jUnit4) {
		super(test);
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

	protected void setUp() throws Exception {
		if (fJUnit4) {
			fgProject= JavaProjectHelper.createJavaProject(PROJECT_NAME_4, "bin");
			JavaProjectHelper.addRTJar(fgProject);
			IClasspathEntry cpe= JavaCore.newContainerEntry(JUnitContainerInitializer.JUNIT4_PATH);
			JavaProjectHelper.addToClasspath(fgProject, cpe);

		} else {
			fgProject= JavaProjectHelper.createJavaProject(PROJECT_NAME_3, "bin");
			JavaProjectHelper.addRTJar13(fgProject);
			IClasspathEntry cpe= JavaCore.newContainerEntry(JUnitContainerInitializer.JUNIT3_PATH);
			JavaProjectHelper.addToClasspath(fgProject, cpe);
		}
		fgRoot= JavaProjectHelper.addSourceContainer(fgProject, SRC_NAME);
		JavaProjectHelper.importResources((IFolder) fgRoot.getResource(), JavaTestPlugin.getDefault().getBundle(), getProjectPath() + SRC_NAME);
	}

	protected void tearDown() throws Exception {
		JavaProjectHelper.delete(fgProject);
		fgProject= null;
		fgRoot= null;
	}

}
