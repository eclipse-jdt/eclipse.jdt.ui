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

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.resource.RenameResourceChange;

public class RenameResourceChangeTests extends RefactoringTest {

	private static final Class clazz= RenameResourceChangeTests.class;
	public RenameResourceChangeTests(String name) {
		super(name);
	}

	public static Test suite() {
		return new RefactoringTestSetup(new TestSuite(clazz));
	}

	public static Test setUpTest(Test test) {
		return new RefactoringTestSetup(test);
	}

	public void testFile0() throws Exception{
		IFolder folder= (IFolder)getPackageP().getCorrespondingResource();
		String newName= "b.txt";
		try{

			String oldName= "a.txt";
			IFile file= folder.getFile(oldName);
			assertTrue("should not exist", ! file.exists());
			String content= "aaaaaaaaa";
			file.create(getStream(content), true, new NullProgressMonitor());
			assertTrue("should exist", file.exists());

			Change change= new RenameResourceChange(file.getFullPath(), newName);
			change.initializeValidationData(new NullProgressMonitor());
			performChange(change);
			assertTrue("after: should exist", folder.getFile(newName).exists());
			assertTrue("after: old should not exist", ! folder.getFile(oldName).exists());
		} finally{
			performDummySearch();
			folder.getFile(newName).delete(true, false, new NullProgressMonitor());
		}
	}

	public void testFile1() throws Exception{

		IFolder folder= (IFolder)getPackageP().getCorrespondingResource();
		String newName= "b.txt";
		try{
			String oldName= "a.txt";
			IFile file= folder.getFile(oldName);
			assertTrue("should not exist", ! file.exists());
			String content= "";
			file.create(getStream(content), true, new NullProgressMonitor());
			assertTrue("should exist", file.exists());


			Change change= new RenameResourceChange(file.getFullPath(), newName);
			change.initializeValidationData(new NullProgressMonitor());
			performChange(change);
			assertTrue("after: should exist", folder.getFile(newName).exists());
			assertTrue("after: old should not exist", ! folder.getFile(oldName).exists());
		} finally{
			performDummySearch();
			folder.getFile(newName).delete(true, false, new NullProgressMonitor());
		}
	}

	public void testFile2() throws Exception{
		IFolder folder= (IFolder)getPackageP().getCorrespondingResource();
		String oldName= "a.txt";
		String newName= "b.txt";
		try{
			IFile file= folder.getFile(oldName);
			assertTrue("should not exist", ! file.exists());
			String content= "aaaaaaaaa";
			file.create(getStream(content), true, new NullProgressMonitor());
			assertTrue("should exist", file.exists());

			Change change= new RenameResourceChange(file.getFullPath(), newName);
			change.initializeValidationData(new NullProgressMonitor());
			Change undo= performChange(change);
			assertTrue("after: should exist", folder.getFile(newName).exists());
			assertTrue("after: old should not exist", ! folder.getFile(oldName).exists());
			//------

			assertTrue("should be undoable", undo != null);
			undo.initializeValidationData(new NullProgressMonitor());
			performChange(undo);
			assertTrue("after undo: should exist", folder.getFile(oldName).exists());
			assertTrue("after undo: old should not exist", ! folder.getFile(newName).exists());
		} finally{
			performDummySearch();
			folder.getFile(oldName).delete(true, false, new NullProgressMonitor());
		}
	}


	public void testFolder0() throws Exception{
		IFolder folder= (IFolder)getPackageP().getCorrespondingResource();
		String newName= "b";
		try{
			String oldName= "a";
			IFolder subFolder= folder.getFolder(oldName);
			assertTrue("should not exist", ! subFolder.exists());
			subFolder.create(true, true, null);
			assertTrue("should exist", subFolder.exists());


			Change change= new RenameResourceChange(subFolder.getFullPath(), newName);
			change.initializeValidationData(new NullProgressMonitor());
			performChange(change);
			assertTrue("after: should exist", folder.getFolder(newName).exists());
			assertTrue("after: old should not exist", ! folder.getFolder(oldName).exists());
		} finally{
			performDummySearch();
			folder.getFolder(newName).delete(true, false, new NullProgressMonitor());
		}
	}

	public void testFolder1() throws Exception{
		IFolder folder= (IFolder)getPackageP().getCorrespondingResource();
		String newName= "b";

		try{
			String oldName= "a";
			IFolder subFolder= folder.getFolder(oldName);
			assertTrue("should not exist", ! subFolder.exists());
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
			assertTrue("after: old should not exist", ! folder.getFolder(oldName).exists());
			assertEquals("after: child count", 2, folder.getFolder(newName).members().length);
		} finally{
			performDummySearch();
			folder.getFolder(newName).delete(true, false, new NullProgressMonitor());
		}
	}

	public void testFolder2() throws Exception{
		IFolder folder= (IFolder)getPackageP().getCorrespondingResource();
		String oldName= "a";
		String newName= "b";
		try{
			IFolder subFolder= folder.getFolder(oldName);
			assertTrue("should not exist", ! subFolder.exists());
			subFolder.create(true, true, null);
			assertTrue("should exist", subFolder.exists());


			Change change= new RenameResourceChange(subFolder.getFullPath(), newName);
			change.initializeValidationData(new NullProgressMonitor());
			Change undo= performChange(change);
			assertTrue("after: should exist", folder.getFolder(newName).exists());
			assertTrue("after: old should not exist", ! folder.getFolder(oldName).exists());

			//---
			assertTrue("should be undoable", undo != null);
			undo.initializeValidationData(new NullProgressMonitor());
			performChange(undo);
			assertTrue("after undo: should exist", folder.getFolder(oldName).exists());
			assertTrue("after undo: old should not exist", ! folder.getFolder(newName).exists());
		} finally{
			performDummySearch();
			folder.getFolder(oldName).delete(true, false, new NullProgressMonitor());
		}
	}

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
			performDummySearch();
			if (project.exists())
				project.delete(true, null);
			if (project2.exists())
				project2.delete(true, null);
		}

	}
}

