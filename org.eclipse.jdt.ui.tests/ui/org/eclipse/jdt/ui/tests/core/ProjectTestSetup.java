/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.ui.tests.core;

import junit.extensions.TestSetup;
import junit.framework.Test;

import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;


public class ProjectTestSetup extends TestSetup {

	public static final String PROJECT_NAME= "TestSetupProject";
	
	public static IJavaProject getProject() {
		IProject project= ResourcesPlugin.getWorkspace().getRoot().getProject(PROJECT_NAME);
		return JavaCore.create(project);
	}
	
	public static IClasspathEntry[] getDefaultClasspath() {
		IPath[] rtJarPath= JavaProjectHelper.findRtJar();
		assertTrue("rt not found", rtJarPath != null);		
		return new IClasspathEntry[] {  JavaCore.newLibraryEntry(rtJarPath[0], rtJarPath[1], rtJarPath[2], true) };
	}
	
		
	private IJavaProject fJProject;

	private boolean fAutobuilding;
	
	public ProjectTestSetup(Test test) {
		super(test);
	}
	
	/* (non-Javadoc)
	 * @see junit.extensions.TestSetup#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		
		IJavaProject project= getProject();
		if (project.exists()) { // allow nesting of ProjectTestSetup's
			return;
		}
		
		fAutobuilding = JavaProjectHelper.setAutoBuilding(false);
		
		fJProject= JavaProjectHelper.createJavaProject(PROJECT_NAME, "bin");
		fJProject.setRawClasspath(getDefaultClasspath(), null);
		JavaCore.setOptions(TestOptions.getDefault());
		JavaPlugin.getDefault().getCodeTemplateStore().restoreDefaults();		
	}

	protected void tearDown() throws Exception {
		if (fJProject != null) {
			JavaProjectHelper.delete(fJProject);
			JavaProjectHelper.setAutoBuilding(fAutobuilding);
		}
	}

}
