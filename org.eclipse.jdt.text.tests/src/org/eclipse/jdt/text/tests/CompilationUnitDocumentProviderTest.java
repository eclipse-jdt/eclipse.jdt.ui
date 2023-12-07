/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.jdt.text.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.core.filebuffers.tests.ResourceHelper;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.part.FileEditorInput;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.ICompilationUnitDocumentProvider;


@SuppressWarnings("restriction") // org.eclipse.core.filebuffers.tests.ResourceHelper
public class CompilationUnitDocumentProviderTest {

	private IJavaProject fJavaProject;
	private IProject fLinkedProject;

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
	 *
	 * @throws CoreException if deletion fails
	 */
	@After
	public void tearDown() throws CoreException {
		if (fJavaProject != null)
			JavaProjectHelper.delete(fJavaProject);

		if (fLinkedProject != null)
			ResourceHelper.delete(fLinkedProject, false);
	}

	@Test
	public void test1() throws Exception {
		setupProject();
		IFile file= ResourcesPlugin.getWorkspace().getRoot().getFile(new Path("/P/src/testA/testB/A.java"));
		checkFile(file);
	}

	@Test
	public void test2() throws Exception {
		setupProject();
		IProject project= (IProject) fJavaProject.getUnderlyingResource();
		IFolder folder= ResourceHelper.createLinkedFolder(project, new Path("src2"), JdtTextTestPlugin.getDefault(), new Path("testResources/folderLinkTarget1"));
		assertNotNull(folder);
		assertTrue(folder.exists());

		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		IFile file= root.getFile(new Path("/P/src2/test1/test2/C.java"));
		checkFile(file);
	}

	@Test
	public void test3() throws Exception {
		fLinkedProject= ResourceHelper.createLinkedProject("P2", JdtTextTestPlugin.getDefault(), new Path("testResources/folderLinkTarget1"));
		assertNotNull(fLinkedProject);
		assertTrue(fLinkedProject.exists());

		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		IFile file= root.getFile(new Path("/P2/test1/test2/C.java"));
		checkFile(file);
	}

	@Test
	public void testNewFile() throws Exception {
		setupProject();
		String source= "// some source";

		IFolder folder= ResourcesPlugin.getWorkspace().getRoot().getFolder(new Path("/P/src/testA/testB/"));
		IPackageFragment frag= (IPackageFragment)JavaCore.create(folder);
		ICompilationUnit cu= frag.getCompilationUnit("New.java");
		cu.becomeWorkingCopy(null);
		IBuffer b= cu.getBuffer();
		b.setContents(source);
		cu.reconcile(ICompilationUnit.NO_AST, true, null, null);
		cu.commitWorkingCopy(true, null);
		cu.discardWorkingCopy();

		cu= frag.getCompilationUnit("New.java");

		assertEquals(source, cu.getSource());
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
