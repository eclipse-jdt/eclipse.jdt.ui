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
package org.eclipse.jdt.text.tests;

import junit.framework.TestCase;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.filebuffers.tests.TestHelper;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.part.FileEditorInput;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.ICompilationUnitDocumentProvider;


public class ComilationUnitDocumentProviderTest extends TestCase {
	
	private IJavaProject fJavaProject;
	private IProject fLinkedProject;

	public ComilationUnitDocumentProviderTest(String name) {
		super(name);
	}
			
	/**
	 * Creates a new test Java project.
	 */	
	protected void setUp() throws Exception {
		fJavaProject= JavaProjectHelper.createJavaProject("P", "bin");
	}

	private void setupProject() throws CoreException, JavaModelException {
		fJavaProject= JavaProjectHelper.createJavaProject("P", "bin");

		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fJavaProject, "src");
		IPackageFragment pack= root.createPackageFragment("testA.testB", true, null);
		
		ICompilationUnit cu= pack.getCompilationUnit("A.java");
		IType type= cu.createType("public class A {\n}\n", null, true, null);
		type.createMethod("public void a() {}\n", null, true, null);
		type.createMethod("public void b(java.util.Vector v) {}\n", null, true, null);
	}

	/**
	 * Removes the test java project.
	 */	
	protected void tearDown () throws Exception {
		if (fJavaProject != null)
			JavaProjectHelper.delete(fJavaProject);
		
		if (fLinkedProject != null)
			TestHelper.delete(fLinkedProject, false);
	}
				
	public void test1() throws Exception {
		setupProject();
		IFile file= ResourcesPlugin.getWorkspace().getRoot().getFile(new Path("/P/src/testA/testB/A.java"));
		checkFile(file);
	}

	public void test2() throws Exception {
		setupProject();
		IProject project= (IProject) fJavaProject.getUnderlyingResource();
		IFolder folder= TestHelper.createLinkedFolder(project, new Path("src2"), JdtTextTestPlugin.getDefault(), new Path("testResources/folderLinkTarget1"));
		assertNotNull(folder);
		assertTrue(folder.exists());

		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		IFile file= root.getFile(new Path("/P/src2/test1/test2/C.java"));
		checkFile(file);
	}
	
	public void test3() throws Exception {
		fLinkedProject= TestHelper.createLinkedProject("P2", JdtTextTestPlugin.getDefault(), new Path("testResources/folderLinkTarget1"));
		assertNotNull(fLinkedProject);
		assertTrue(fLinkedProject.exists());
		
		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		IFile file= root.getFile(new Path("/P2/test1/test2/C.java"));
		checkFile(file);
	}
	
	private void checkFile(IFile file) throws CoreException {
		assertNotNull(file);
		IEditorInput input= new FileEditorInput(file);
		
		ICompilationUnitDocumentProvider provider= JavaPlugin.getDefault().getCompilationUnitDocumentProvider();
		assertNotNull(provider);
		
		provider.connect(input);		
		assertNotNull(provider.getDocument(input));
		assertNotNull(provider.getAnnotationModel(input));
		provider.disconnect(input);
		assertNull(provider.getDocument(input));
		assertNull(provider.getAnnotationModel(input));
	}
}
