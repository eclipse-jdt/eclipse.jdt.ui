/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.refactoring.changes.RenameSourceFolderChange;

import org.eclipse.jdt.ui.tests.refactoring.rules.RefactoringTestSetup;

public class RenameSourceFolderChangeTests extends GenericRefactoringTest {

	public RenameSourceFolderChangeTests() {
		rts= new RefactoringTestSetup();
	}

	@Test
	public void test0() throws Exception {
		String oldName= "oldName";
		String newName= "newName";

		try{
			IJavaProject testProject= rts.getProject();
			IPackageFragmentRoot oldRoot= JavaProjectHelper.addSourceContainer(rts.getProject(), oldName);

			assertTrue("old folder should exist here", oldRoot.exists());

			RenameSourceFolderChange change= new RenameSourceFolderChange(oldRoot, newName);
			change.initializeValidationData(new NullProgressMonitor());
			performChange(change);

			assertFalse("old folder should not exist", oldRoot.exists());
			assertEquals("expected 3 pfr's", 3, testProject.getPackageFragmentRoots().length);
			IPackageFragmentRoot[] newRoots= testProject.getPackageFragmentRoots();
			for (int i= 0; i < newRoots.length; i++){
				assertTrue("should exist " + i, newRoots[i].exists());
			}
		} finally{
			JavaProjectHelper.removeSourceContainer(rts.getProject(), newName);
		}
	}

	@Test
	public void test1() throws Exception {
		String oldName1= "oldName1";
		String oldName2= "oldName2";
		String newName1= "newName";

		try{

			IJavaProject testProject= rts.getProject();
			IPackageFragmentRoot oldRoot1= JavaProjectHelper.addSourceContainer(rts.getProject(), oldName1);
			IPackageFragmentRoot oldRoot2= JavaProjectHelper.addSourceContainer(rts.getProject(), oldName2);

			assertTrue("old folder should exist here", oldRoot1.exists());
			assertTrue("old folder 2 should exist here", oldRoot2.exists());

			RenameSourceFolderChange change= new RenameSourceFolderChange(oldRoot1, newName1);
			change.initializeValidationData(new NullProgressMonitor());
			performChange(change);

			assertFalse("old folder should not exist", oldRoot1.exists());
			assertEquals("expected 4 pfr's", 4, testProject.getPackageFragmentRoots().length);
			IPackageFragmentRoot[] newRoots= testProject.getPackageFragmentRoots();
			for (int i= 0; i < newRoots.length; i++){
				//DebugUtils.dump(newRoots[i].getElementName());
				assertTrue("should exist " + i, newRoots[i].exists());
				if (i == 2)
					assertEquals("3rd position should be:" + newName1, newName1, newRoots[i].getElementName());
			}
		}finally{
			JavaProjectHelper.removeSourceContainer(rts.getProject(), newName1);
			JavaProjectHelper.removeSourceContainer(rts.getProject(), oldName2);
		}
	}

	@Test
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

			assertFalse("src should not exist", src.exists());
			assertEquals("expected 2 pfr's", 2, project.getPackageFragmentRoots().length);

			IClasspathEntry[] rawClasspath= project.getRawClasspath();
			assertEquals(projectPath, rawClasspath[0].getPath());

			assertEquals("src2/", rawClasspath[0].getExclusionPatterns()[0].toString());
			assertEquals(projectPath.append("src2/"), rawClasspath[1].getPath());
		} finally {
			JavaProjectHelper.delete(project.getProject());
		}
	}
}
