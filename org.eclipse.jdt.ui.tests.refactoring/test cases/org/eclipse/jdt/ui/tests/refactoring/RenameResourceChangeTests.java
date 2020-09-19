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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.resource.RenameResourceChange;

import org.eclipse.jdt.ui.tests.refactoring.rules.RefactoringTestSetup;

public class RenameResourceChangeTests extends GenericRefactoringTest {

	public RenameResourceChangeTests() {
		rts= new RefactoringTestSetup();
	}

	@Test
	public void testFile0() throws Exception{
		IFolder folder= (IFolder)getPackageP().getCorrespondingResource();
		String newName= "b.txt";
		String oldName= "a.txt";
		IFile file= folder.getFile(oldName);
		assertFalse("should not exist", file.exists());
		String content= "aaaaaaaaa";
		file.create(getStream(content), true, new NullProgressMonitor());
		assertTrue("should exist", file.exists());

		Change change= new RenameResourceChange(file.getFullPath(), newName);
		change.initializeValidationData(new NullProgressMonitor());
		performChange(change);
		assertTrue("after: should exist", folder.getFile(newName).exists());
		assertFalse("after: old should not exist", folder.getFile(oldName).exists());
	}

	@Test
	public void testFile1() throws Exception{

		IFolder folder= (IFolder)getPackageP().getCorrespondingResource();
		String newName= "b.txt";
		String oldName= "a.txt";
		IFile file= folder.getFile(oldName);
		assertFalse("should not exist", file.exists());
		String content= "";
		file.create(getStream(content), true, new NullProgressMonitor());
		assertTrue("should exist", file.exists());


		Change change= new RenameResourceChange(file.getFullPath(), newName);
		change.initializeValidationData(new NullProgressMonitor());
		performChange(change);
		assertTrue("after: should exist", folder.getFile(newName).exists());
		assertFalse("after: old should not exist", folder.getFile(oldName).exists());
	}

	@Test
	public void testFile2() throws Exception{
		IFolder folder= (IFolder)getPackageP().getCorrespondingResource();
		String oldName= "a.txt";
		String newName= "b.txt";
		IFile file= folder.getFile(oldName);
		assertFalse("should not exist", file.exists());
		String content= "aaaaaaaaa";
		file.create(getStream(content), true, new NullProgressMonitor());
		assertTrue("should exist", file.exists());

		Change change= new RenameResourceChange(file.getFullPath(), newName);
		change.initializeValidationData(new NullProgressMonitor());
		Change undo= performChange(change);
		assertTrue("after: should exist", folder.getFile(newName).exists());
		assertFalse("after: old should not exist", folder.getFile(oldName).exists());
		//------

		assertNotNull("should be undoable", undo);
		undo.initializeValidationData(new NullProgressMonitor());
		performChange(undo);
		assertTrue("after undo: should exist", folder.getFile(oldName).exists());
		assertFalse("after undo: old should not exist", folder.getFile(newName).exists());
	}


	@Test
	public void testFolder0() throws Exception{
		IFolder folder= (IFolder)getPackageP().getCorrespondingResource();
		String newName= "b";
		String oldName= "a";
		IFolder subFolder= folder.getFolder(oldName);
		assertFalse("should not exist", subFolder.exists());
		subFolder.create(true, true, null);
		assertTrue("should exist", subFolder.exists());


		Change change= new RenameResourceChange(subFolder.getFullPath(), newName);
		change.initializeValidationData(new NullProgressMonitor());
		performChange(change);
		assertTrue("after: should exist", folder.getFolder(newName).exists());
		assertFalse("after: old should not exist", folder.getFolder(oldName).exists());
	}

	@Test
	public void testFolder1() throws Exception{
		IFolder folder= (IFolder)getPackageP().getCorrespondingResource();
		String newName= "b";

		String oldName= "a";
		IFolder subFolder= folder.getFolder(oldName);
		assertFalse("should not exist", subFolder.exists());
		subFolder.create(true, true, null);
		IFile file1= subFolder.getFile("a.txt");
		IFile file2= subFolder.getFile("b.txt");
		file1.create(getStream("123"), true, null);
		file2.create(getStream("123345"), true, null);

		assertTrue("should exist", subFolder.exists());
		assertTrue("file1 should exist", file1.exists());
		assertTrue("file2 should exist", file2.exists());

		Change change= new RenameResourceChange(subFolder.getFullPath(), newName);
		change.initializeValidationData(new NullProgressMonitor());
		performChange(change);
		assertTrue("after: should exist", folder.getFolder(newName).exists());
		assertFalse("after: old should not exist", folder.getFolder(oldName).exists());
		assertEquals("after: child count", 2, folder.getFolder(newName).members().length);
	}

	@Test
	public void testFolder2() throws Exception{
		IFolder folder= (IFolder)getPackageP().getCorrespondingResource();
		String oldName= "a";
		String newName= "b";
		IFolder subFolder= folder.getFolder(oldName);
		assertFalse("should not exist", subFolder.exists());
		subFolder.create(true, true, null);
		assertTrue("should exist", subFolder.exists());


		Change change= new RenameResourceChange(subFolder.getFullPath(), newName);
		change.initializeValidationData(new NullProgressMonitor());
		Change undo= performChange(change);
		assertTrue("after: should exist", folder.getFolder(newName).exists());
		assertFalse("after: old should not exist", folder.getFolder(oldName).exists());

		//---
		assertNotNull("should be undoable", undo);
		undo.initializeValidationData(new NullProgressMonitor());
		performChange(undo);
		assertTrue("after undo: should exist", folder.getFolder(oldName).exists());
		assertFalse("after undo: old should not exist", folder.getFolder(newName).exists());
	}

	@Test
	public void testJavaProject01() throws Exception {
		String oldName= "RenameResourceChangeTest";
		String newName= "RenameResourceChangeTest2";
		String linkName= "link";

		IWorkspaceRoot workspaceRoot= ResourcesPlugin.getWorkspace().getRoot();
		IProject project= workspaceRoot.getProject(oldName);
		IProject project2= workspaceRoot.getProject(newName);
		try {
			getPackageP().createCompilationUnit("A.java", "package p;\nclass A{}\n", false, null);

			project.create(null);
			project.open(null);
			IFolder link= project.getFolder(linkName);
			link.createLink(getPackageP().getResource().getRawLocation(), IResource.NONE, null);
			assertTrue(link.exists());

			RenameResourceChange change= new RenameResourceChange(project.getFullPath(), newName);
			change.initializeValidationData(new NullProgressMonitor());
			performChange(change);

			assertTrue("after: linked folder should exist", project2.getFolder(linkName).exists());
			assertTrue("after: linked folder should be linked", project2.getFolder(linkName).isLinked());
			assertTrue("after: linked folder should contain cu", project2.getFolder(linkName).getFile("A.java").exists());
		} finally {
			if (project.exists())
				JavaProjectHelper.delete(project);
			if (project2.exists())
				JavaProjectHelper.delete(project2);
		}
	}
}
