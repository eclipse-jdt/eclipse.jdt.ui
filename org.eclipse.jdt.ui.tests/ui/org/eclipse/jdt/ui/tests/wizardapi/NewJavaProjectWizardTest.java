/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.wizardapi;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.ui.actions.WorkspaceModifyDelegatingOperation;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.launching.JavaRuntime;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPage;

public class NewJavaProjectWizardTest extends TestCase {


	public static Test suite() {
		return new TestSuite(NewJavaProjectWizardTest.class);
	}

	private class TestNewJavaProjectWizardPage extends NewJavaProjectWizardPage {

		private IProject fNewProject;

		public TestNewJavaProjectWizardPage(IWorkspaceRoot root) {
			super(root, null);
		}

		public void setProjectHandle(IProject newProject) {
			fNewProject= newProject;
		}

		/**
		 * @see NewJavaProjectWizardPage#getLocationPath()
		 */
		protected IPath getLocationPath() {
			return null;
		}

		/**
		 * @see NewJavaProjectWizardPage#getProjectHandle()
		 */
		protected IProject getProjectHandle() {
			return fNewProject;
		}

		public void initBuildPath() {
			super.initBuildPaths();
		}

	}


	private static final String PROJECT_NAME = "DummyProject";
	private static final String OTHER_PROJECT_NAME = "OtherProject";


	private TestNewJavaProjectWizardPage fWizardPage;

	public NewJavaProjectWizardTest(String name) {
		super(name);
	}

	/**
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();

		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();

		IProject project= root.getProject(PROJECT_NAME);

		fWizardPage= new TestNewJavaProjectWizardPage(root);
		fWizardPage.setProjectHandle(project);
	}

	/**
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		IProject project= root.getProject(PROJECT_NAME);
		if (project.exists()) {
			project.delete(true, null);
		}

		project= root.getProject(OTHER_PROJECT_NAME);
		if (project.exists()) {
			project.delete(true, null);
		}

		super.tearDown();
	}

	private IPath getJREEntryPath() {
		return JavaRuntime.getDefaultJREContainerEntry().getPath();
	}


	private void assertBasicBuildPath(IProject project, IPath outputLocation, IClasspathEntry[] classpath) {
		assertNotNull("a", outputLocation);
		assertNotNull("b", classpath);
		assertTrue("c", classpath.length == 2);

		if (PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.SRCBIN_FOLDERS_IN_NEWPROJ)) {
			assertEquals("d", outputLocation, project.getFolder("bin").getFullPath());
			assertEquals("e", classpath[0].getPath(), project.getFolder("src").getFullPath());
		} else {
			assertEquals("f", outputLocation, project.getFullPath());
			assertEquals("g", classpath[0].getPath(), project.getFullPath());
		}
		assertEquals("h", classpath[1].getPath(), getJREEntryPath());
	}

	public void testBasicSet() throws Exception {
		fWizardPage.initBuildPath();
		IProject project= fWizardPage.getProjectHandle();

		IPath outputLocation= fWizardPage.getOutputLocation();
		IClasspathEntry[] classpath= fWizardPage.getRawClassPath();
		assertBasicBuildPath(project, outputLocation, classpath);
	}


	public void testBasicCreate() throws Exception {
		IProject project= fWizardPage.getProjectHandle();

		IRunnableWithProgress op= new WorkspaceModifyDelegatingOperation(fWizardPage.getRunnable());
		op.run(null);

		IJavaProject jproj= fWizardPage.getNewJavaProject();

		assertEquals("a", jproj.getProject(), project);

		IPath outputLocation= jproj.getOutputLocation();
		IClasspathEntry[] classpath= jproj.getRawClasspath();
		assertBasicBuildPath(jproj.getProject(), outputLocation, classpath);
	}

	public void testProjectChange() throws Exception {
		fWizardPage.initBuildPath();
		IProject project= fWizardPage.getProjectHandle();

		IPath outputLocation= fWizardPage.getOutputLocation();
		IClasspathEntry[] classpath= fWizardPage.getRawClassPath();
		assertBasicBuildPath(project, outputLocation, classpath);

		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		IProject otherProject= root.getProject(OTHER_PROJECT_NAME);

		// change the project before create
		fWizardPage.setProjectHandle(otherProject);

		IRunnableWithProgress op= new WorkspaceModifyDelegatingOperation(fWizardPage.getRunnable());
		op.run(null);

		IJavaProject jproj= fWizardPage.getNewJavaProject();

		assertEquals("a", jproj.getProject(), otherProject);

		IPath outputLocation1= fWizardPage.getOutputLocation();
		IClasspathEntry[] classpath1= fWizardPage.getRawClassPath();
		assertBasicBuildPath(otherProject, outputLocation1, classpath1);
	}

	private void assertUserBuildPath(IProject project, IPath outputLocation, IClasspathEntry[] classpath) {
		assertNotNull("a", outputLocation);
		assertNotNull("b", classpath);
		assertTrue("c", classpath.length == 3);

		assertEquals("d", outputLocation, project.getFolder("dbin").getFullPath());
		assertEquals("e", classpath[0].getPath(), project.getFolder("dsrc1").getFullPath());
		assertEquals("f", classpath[1].getPath(), project.getFolder("dsrc2").getFullPath());
		assertEquals("g", classpath[2].getPath(), getJREEntryPath());
	}

	public void testUserSet() throws Exception {
		IProject project= fWizardPage.getProjectHandle();

		IPath folderPath= project.getFolder("dbin").getFullPath();

		IClasspathEntry[] entries= new IClasspathEntry[] {
			JavaCore.newSourceEntry(project.getFolder("dsrc1").getFullPath()),
			JavaCore.newSourceEntry(project.getFolder("dsrc2").getFullPath())
		};

		fWizardPage.setDefaultOutputFolder(folderPath);
		fWizardPage.setDefaultClassPath(entries, true);
		fWizardPage.initBuildPath();

		IPath outputLocation= fWizardPage.getOutputLocation();
		IClasspathEntry[] classpath= fWizardPage.getRawClassPath();
		assertUserBuildPath(project, outputLocation, classpath);

		fWizardPage.setDefaultOutputFolder(null);
		fWizardPage.setDefaultClassPath(null, false);
		fWizardPage.initBuildPath();

		IPath outputLocation1= fWizardPage.getOutputLocation();
		IClasspathEntry[] classpath1= fWizardPage.getRawClassPath();
		assertBasicBuildPath(project, outputLocation1, classpath1);
	}

	public void testUserCreate() throws Exception {
		IProject project= fWizardPage.getProjectHandle();

		IPath folderPath= project.getFolder("dbin").getFullPath();

		IClasspathEntry[] entries= new IClasspathEntry[] {
			JavaCore.newSourceEntry(project.getFolder("dsrc1").getFullPath()),
			JavaCore.newSourceEntry(project.getFolder("dsrc2").getFullPath())
		};

		fWizardPage.setDefaultOutputFolder(folderPath);
		fWizardPage.setDefaultClassPath(entries, true);

		IRunnableWithProgress op= new WorkspaceModifyDelegatingOperation(fWizardPage.getRunnable());
		op.run(null);

		IJavaProject jproj= fWizardPage.getNewJavaProject();

		assertEquals("a", jproj.getProject(), project);

		IPath outputLocation= jproj.getOutputLocation();
		IClasspathEntry[] classpath= jproj.getRawClasspath();
		assertUserBuildPath(jproj.getProject(), outputLocation, classpath);
	}

	public void testReadExisting() throws Exception {
		IProject project= fWizardPage.getProjectHandle();

		IPath folderPath= project.getFolder("dbin").getFullPath();
		IClasspathEntry[] entries= new IClasspathEntry[] {
			JavaCore.newSourceEntry(project.getFolder("dsrc1").getFullPath()),
			JavaCore.newSourceEntry(project.getFolder("dsrc2").getFullPath())
		};

		fWizardPage.setDefaultOutputFolder(folderPath);
		fWizardPage.setDefaultClassPath(entries, true);

		IRunnableWithProgress op= new WorkspaceModifyDelegatingOperation(fWizardPage.getRunnable());
		op.run(null);

		IProject proj= fWizardPage.getNewJavaProject().getProject();

		fWizardPage.setDefaultClassPath(null, false);
		fWizardPage.setDefaultOutputFolder(null);
		fWizardPage.setProjectHandle(proj);

		// reads from existing
		fWizardPage.initBuildPath();

		IPath outputLocation1= fWizardPage.getOutputLocation();
		IClasspathEntry[] classpath1= fWizardPage.getRawClassPath();
		assertUserBuildPath(project, outputLocation1, classpath1);
	}

	public void testExistingOverwrite() throws Exception {
		IProject project= fWizardPage.getProjectHandle();

		IRunnableWithProgress op= new WorkspaceModifyDelegatingOperation(fWizardPage.getRunnable());
		op.run(null);

		IPath folderPath= project.getFolder("dbin").getFullPath();
		IClasspathEntry[] entries= new IClasspathEntry[] {
			JavaCore.newSourceEntry(project.getFolder("dsrc1").getFullPath()),
			JavaCore.newSourceEntry(project.getFolder("dsrc2").getFullPath())
		};

		fWizardPage.setDefaultOutputFolder(folderPath);
		fWizardPage.setDefaultClassPath(entries, true);

		// should overwrite existing
		IRunnableWithProgress op1= new WorkspaceModifyDelegatingOperation(fWizardPage.getRunnable());
		op1.run(null);

		IJavaProject jproj= fWizardPage.getNewJavaProject();

		IPath outputLocation1= jproj.getOutputLocation();
		IClasspathEntry[] classpath1= jproj.getRawClasspath();
		assertUserBuildPath(project, outputLocation1, classpath1);
	}
}



