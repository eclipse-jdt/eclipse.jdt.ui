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
package org.eclipse.jdt.ui.tests.refactoring;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.refactoring.changes.RenameSourceFolderChange;


public class RenameSourceFolderChangeTests extends RefactoringTest {

	private static final Class clazz= RenameSourceFolderChangeTests.class;

	public RenameSourceFolderChangeTests(String name){
		super(name);
	}

	public static Test suite() {
		return new RefactoringTestSetup(new TestSuite(clazz));
	}

	public static Test setUpTest(Test test) {
		return new RefactoringTestSetup(test);
	}

	public void test0() throws Exception {
		String oldName= "oldName";
		String newName= "newName";

		try{
			IJavaProject testProject= RefactoringTestSetup.getProject();
			IPackageFragmentRoot oldRoot= JavaProjectHelper.addSourceContainer(RefactoringTestSetup.getProject(), oldName);

			assertTrue("old folder should exist here", oldRoot.exists());

			RenameSourceFolderChange change= new RenameSourceFolderChange(oldRoot, newName);
			change.initializeValidationData(new NullProgressMonitor());
			performChange(change);

			assertTrue("old folder should not exist", ! oldRoot.exists());
			assertEquals("expected 3 pfr's", 3, testProject.getPackageFragmentRoots().length);
			IPackageFragmentRoot[] newRoots= testProject.getPackageFragmentRoots();
			for (int i= 0; i < newRoots.length; i++){
				assertTrue("should exist " + i, newRoots[i].exists());
			}
		} finally{
			JavaProjectHelper.removeSourceContainer(RefactoringTestSetup.getProject(), newName);
		}
	}

	public void test1() throws Exception {
		String oldName1= "oldName1";
		String oldName2= "oldName2";
		String newName1= "newName";

		try{

			IJavaProject testProject= RefactoringTestSetup.getProject();
			IPackageFragmentRoot oldRoot1= JavaProjectHelper.addSourceContainer(RefactoringTestSetup.getProject(), oldName1);
			IPackageFragmentRoot oldRoot2= JavaProjectHelper.addSourceContainer(RefactoringTestSetup.getProject(), oldName2);

			assertTrue("old folder should exist here", oldRoot1.exists());
			assertTrue("old folder 2 should exist here", oldRoot2.exists());

			RenameSourceFolderChange change= new RenameSourceFolderChange(oldRoot1, newName1);
			change.initializeValidationData(new NullProgressMonitor());
			performChange(change);

			assertTrue("old folder should not exist", ! oldRoot1.exists());
			assertEquals("expected 4 pfr's", 4, testProject.getPackageFragmentRoots().length);
			IPackageFragmentRoot[] newRoots= testProject.getPackageFragmentRoots();
			for (int i= 0; i < newRoots.length; i++){
				//DebugUtils.dump(newRoots[i].getElementName());
				assertTrue("should exist " + i, newRoots[i].exists());
				if (i == 2)
					assertEquals("3rd position should be:" + newName1, newName1, newRoots[i].getElementName());
			}
		}finally{
			JavaProjectHelper.removeSourceContainer(RefactoringTestSetup.getProject(), newName1);
			JavaProjectHelper.removeSourceContainer(RefactoringTestSetup.getProject(), oldName2);
		}
	}

	public void testBug129991() throws Exception {
		IJavaProject project= JavaProjectHelper.createJavaProject("RenameSourceFolder", "bin");

		try {
			IPath projectPath= project.getPath();

			IPath[] exclusion= new IPath[] { new Path("src/") };
			JavaProjectHelper.addToClasspath(project, JavaCore.newSourceEntry(projectPath, exclusion));
			IPackageFragmentRoot src= JavaProjectHelper.addSourceContainer(project, "src");

			RenameSourceFolderChange change= new RenameSourceFolderChange(src, "src2");
			change.initializeValidationData(new NullProgressMonitor());
			performChange(change);

			assertTrue("src should not exist", ! src.exists());
			assertEquals("expected 2 pfr's", 2, project.getPackageFragmentRoots().length);

			IClasspathEntry[] rawClasspath= project.getRawClasspath();
			assertEquals(projectPath, rawClasspath[0].getPath());

			assertEquals("src2/", rawClasspath[0].getExclusionPatterns()[0].toString());
			assertEquals(projectPath.append("src2/"), rawClasspath[1].getPath());
		} finally {
			project.getProject().delete(true, null);
		}
	}

}

