/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.text.tests.performance;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import junit.extensions.TestSetup;
import junit.framework.Test;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.text.tests.JdtTextTestPlugin;

public class PerformanceTestSetup extends TestSetup {

	private static final String PERSPECTIVE= "org.eclipse.jdt.ui.JavaPerspective";

	public static final String PROJECT= "org.eclipse.swt";
	
	private static final String PROJECT_ZIP= "/testResources/org.eclipse.swt-R3_0.zip";

	private static final String INTRO_VIEW= "org.eclipse.ui.internal.introview";
	
	public PerformanceTestSetup(Test test) {
		super(test);
	}

	/*
	 * @see junit.extensions.TestSetup#setUp()
	 */
	protected void setUp() throws Exception {
		IWorkbench workbench= PlatformUI.getWorkbench();
		IWorkbenchWindow activeWindow= workbench.getActiveWorkbenchWindow();
		IWorkbenchPage activePage= activeWindow.getActivePage();
		
		activePage.hideView(activePage.findViewReference(INTRO_VIEW));
		
		workbench.showPerspective(PERSPECTIVE, activeWindow);
		
		boolean wasAutobuilding= ResourceTestHelper.disableAutoBuilding();
		setUpProject();
		ResourceTestHelper.fullBuild();
		if (wasAutobuilding)
			ResourceTestHelper.enableAutoBuilding();
	}
	
	/*
	 * @see junit.extensions.TestSetup#tearDown()
	 */
	protected void tearDown() throws Exception {
		// do nothing, the set up workspace will be used by the open editor tests
		
		/* 
		 * ensure the workbench state gets saved when running with the Automated Testing Framework
                 * TODO: remove when https://bugs.eclipse.org/bugs/show_bug.cgi?id=71362 is fixed
                 */
		StackTraceElement[] elements=  new Throwable().getStackTrace();
		for (int i= 0; i < elements.length; i++) {
			StackTraceElement element= elements[i];
			if (element.getClassName().equals("org.eclipse.test.EclipseTestRunner")) {
				PlatformUI.getWorkbench().close();
				break;
			}
		}
	}
	
	private void setUpProject() throws IOException, ZipException, CoreException {
		String workspacePath= ResourcesPlugin.getWorkspace().getRoot().getLocation().toString() + "/";
		FileTool.unzip(new ZipFile(FileTool.getFileInPlugin(JdtTextTestPlugin.getDefault(), new Path(PROJECT_ZIP))), new File(workspacePath));
		File oldFile= new File(workspacePath + PROJECT + "/.classpath_win32");
		File newFile= new File(workspacePath + PROJECT + "/.classpath");
		assertTrue(oldFile.renameTo(newFile));

		IProject project= createExistingProject(PROJECT);
		assertTrue(JavaCore.create(project).exists());
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
