/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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

import org.eclipse.core.resources.IFolder;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.junit.buildpath.JUnitContainerInitializer;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

public class JUnitWorkspaceTestSetup extends TestSetup implements Test {

	public static final String PROJECT_NAME= "JUnitTests";
	public static final String PROJECT_PATH= "testresources/JUnitWorkspace/" + PROJECT_NAME + "/";
	public static final String SRC_NAME= "src";
	public static final String SRC_PATH= PROJECT_PATH + SRC_NAME + "/";
	
	private static IJavaProject fgProject;
	private static IPackageFragmentRoot fgRoot;

	public JUnitWorkspaceTestSetup(Test test) {
		super(test);
	}
	
	public static IJavaProject getJavaProject() {
		return fgProject;
	}
	
	public static IPackageFragmentRoot getRoot() {
		return fgRoot;
	}
	
	protected void setUp() throws Exception {
		fgProject= JavaProjectHelper.createJavaProject(PROJECT_NAME, "bin");
		JavaProjectHelper.addRTJar13(fgProject);
		
		IClasspathEntry cpe= JavaCore.newContainerEntry(JUnitContainerInitializer.JUNIT3_PATH);
		JavaProjectHelper.addToClasspath(fgProject, cpe);

		fgRoot= JavaProjectHelper.addSourceContainer(fgProject, SRC_NAME);
		JavaProjectHelper.importResources((IFolder) fgRoot.getResource(), SRC_PATH);
	}
	
	protected void tearDown() throws Exception {
		JavaProjectHelper.delete(fgProject);
		fgProject= null;
	}

}
