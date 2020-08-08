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
package org.eclipse.jdt.ui.tests.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.manipulation.SharedASTProviderCore;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class TypeHierarchyTest {
	@Rule
	public ProjectTestSetup pts= new ProjectTestSetup();

	private IJavaProject fJavaProject1;
	private IJavaProject fJavaProject2;

	@Before
	public void setUp() throws Exception {
		fJavaProject1= pts.getProject();
		fJavaProject2= JavaProjectHelper.createJavaProject("TestProject2", "bin");
	}

	@After
	public void tearDown () throws Exception {
		JavaProjectHelper.clear(fJavaProject1, pts.getDefaultClasspath());
		JavaProjectHelper.delete(fJavaProject2);
	}

	@Test
	public void test1() throws Exception {
		IPackageFragmentRoot root1= JavaProjectHelper.addSourceContainer(fJavaProject1, "src");
		IPackageFragment pack1= root1.createPackageFragment("pack1", true, null);

		ICompilationUnit cu1= pack1.getCompilationUnit("A.java");
		cu1.createType("public class A {\n}\n", null, true, null);

		JavaProjectHelper.addRTJar(fJavaProject2);
		JavaProjectHelper.addRequiredProject(fJavaProject2, fJavaProject1);
		IPackageFragmentRoot root2= JavaProjectHelper.addSourceContainer(fJavaProject2, "src");
		IPackageFragment pack2= root2.createPackageFragment("pack2", true, null);

		ICompilationUnit cu2= pack2.getCompilationUnit("B.java");
		IType type2= cu2.createType("public class B extends pack1.A {\n}\n", null, true, null);

		ITypeHierarchy hierarchy= type2.newSupertypeHierarchy(null);
		IType[] allTypes= hierarchy.getAllTypes();

		assertEquals("Should contain 3 types, contains: " + allTypes.length, 3, allTypes.length);

		IType type= fJavaProject2.findType("pack1.A");
		assertNotNull("Type not found", type);
	}

	@Test
	public void hierarchyWithWorkingCopy1() throws Exception {

		IPackageFragmentRoot root1= JavaProjectHelper.addSourceContainer(fJavaProject1, "src");
		IPackageFragment pack1= root1.createPackageFragment("pack1", true, null);

		ICompilationUnit cu1= pack1.getCompilationUnit("A.java");
		cu1.createType("public class A {\n}\n", null, true, null);

		IPackageFragment pack2= root1.createPackageFragment("pack2", true, null);

		ICompilationUnit cu2= pack2.getCompilationUnit("B.java");
		IType type2= cu2.createType("public class B extends pack1.A {\n}\n", null, true, null);

		final int[] updateCount= new int[] {0};

		ITypeHierarchy hierarchy= type2.newSupertypeHierarchy(null);
		hierarchy.addTypeHierarchyChangedListener(typeHierarchy -> updateCount[0]++);

		IType[] allTypes= hierarchy.getAllTypes();

		assertEquals("Should contain 3 types, contains: " + allTypes.length, 3, allTypes.length);
		assertEquals("Update count should be 0, is: " + updateCount[0], 0, updateCount[0]);

		IEditorPart part= JavaUI.openInEditor(type2);
		try {
			IDocument document= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
			String superType= "pack1.A";

			int offset= document.get().indexOf(superType);

			document.replace(offset, superType.length(), "Object");

			allTypes= hierarchy.getAllTypes();

			// no update of hierarchies on working copies
			assertEquals("Update count should be 0, is: " + updateCount[0], 0, updateCount[0]);
			assertEquals("Should contain 3 types, contains: " + allTypes.length, 3, allTypes.length);

			part.doSave(null);
			SharedASTProviderCore.getAST(cu2, SharedASTProviderCore.WAIT_YES, null);
			hierarchy.refresh(null);

			allTypes= hierarchy.getAllTypes();

			// update after save
			assertEquals("Update count should be 1, is: " + updateCount[0], 1, updateCount[0]);
			hierarchy.refresh(null);
			assertEquals("Should contain 2 types, contains: " + allTypes.length, 2, allTypes.length);

		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
		}

		allTypes= hierarchy.getAllTypes();

		// update after save
		assertEquals("Update count should be 1, is: " + updateCount[0], 1, updateCount[0]);
		assertEquals("Should contain 2 types, contains: " + allTypes.length, 2, allTypes.length);


	}

	@Test
	public void hierarchyWithWorkingCopy2() throws Exception {
		IPackageFragmentRoot root1= JavaProjectHelper.addSourceContainer(fJavaProject1, "src");
		IPackageFragment pack1= root1.createPackageFragment("pack1", true, null);

		ICompilationUnit cu1= pack1.getCompilationUnit("A.java");
		cu1.createType("public class A {\n}\n", null, true, null);

		IPackageFragment pack2= root1.createPackageFragment("pack2", true, null);

		ICompilationUnit cu2= pack2.getCompilationUnit("B.java");
		IType type2= cu2.createType("public class B extends pack1.A {\n}\n", null, true, null);

		IEditorPart part= JavaUI.openInEditor(type2);

		final int[] updateCount= new int[] {0};

		// create on type in working copy
		ITypeHierarchy hierarchy= type2.newSupertypeHierarchy(null);
		hierarchy.addTypeHierarchyChangedListener(typeHierarchy -> updateCount[0]++);

		IType[] allTypes= hierarchy.getAllTypes();

		assertEquals("Update count should be 0, is: " + updateCount[0], 0, updateCount[0]);
		assertEquals("Should contain 3 types, contains: " + allTypes.length, 3, allTypes.length);

		try {

			IDocument document= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
			String superType= "pack1.A";

			int offset= document.get().indexOf(superType);

			document.replace(offset, superType.length(), "Object");

			allTypes= hierarchy.getAllTypes();

			// no update of hierarchies on working copies
			assertEquals("Update count should be 0, is: " + updateCount[0], 0, updateCount[0]);
			assertEquals("Should contain 3 types, contains: " + allTypes.length, 3, allTypes.length);

			part.doSave(null);
			SharedASTProviderCore.getAST(cu2, SharedASTProviderCore.WAIT_YES, null);
			hierarchy.refresh(null);

			allTypes= hierarchy.getAllTypes();

			// update after save
			assertEquals("Update count should be 1, is: " + updateCount[0], 1, updateCount[0]);
			hierarchy.refresh(null);
			assertEquals("Should contain 2 types, contains: " + allTypes.length, 2, allTypes.length);

		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
		}

		allTypes= hierarchy.getAllTypes();

		// update after save
		assertEquals("Update count should be 1, is: " + updateCount[0], 1, updateCount[0]);
		assertEquals("Should contain 2 types, contains: " + allTypes.length, 2, allTypes.length);

	}

	@Test
	public void hierarchyWithWorkingCopy3() throws Exception {
		IPackageFragmentRoot root1= JavaProjectHelper.addSourceContainer(fJavaProject1, "src");
		IPackageFragment pack1= root1.createPackageFragment("pack1", true, null);

		ICompilationUnit cu1= pack1.getCompilationUnit("A.java");
		cu1.createType("public class A {\n}\n", null, true, null);

		IPackageFragment pack2= root1.createPackageFragment("pack2", true, null);

		ICompilationUnit cu2= pack2.getCompilationUnit("B.java");
		IType type2= cu2.createType("public class B extends pack1.A {\n}\n", null, true, null);

		// open editor -> working copy will be created
		IEditorPart part= JavaUI.openInEditor(type2, false, false);

		final int[] updateCount= new int[] {0};

		// create on type in primary working copy
		ITypeHierarchy hierarchy= type2.newSupertypeHierarchy(null);
		hierarchy.addTypeHierarchyChangedListener(typeHierarchy -> updateCount[0]++);

		IType[] allTypes= hierarchy.getAllTypes();

		assertEquals("Should contain 3 types, contains: " + allTypes.length, 3, allTypes.length);
		assertEquals("Update count should be 0, is: " + updateCount[0], 0, updateCount[0]);

		try {
			IDocument document= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
			String superType= "pack1.A";

			int offset= document.get().indexOf(superType);
			// modify source
			document.replace(offset, superType.length(), "Object");

			JavaModelUtil.reconcile(cu2);

			allTypes= hierarchy.getAllTypes();

			// no update of hierarchies on working copies
			assertEquals("Should contain 3 types, contains: " + allTypes.length, 3, allTypes.length);
			assertEquals("Update count should be 0, is: " + updateCount[0], 0, updateCount[0]);

			// no save

		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
		}

		allTypes= hierarchy.getAllTypes();

		// update after close: hierarchy changed because of reconcile delta from closeAllEditors
		assertEquals("Should contain 3 types, contains: " + allTypes.length, 3, allTypes.length);
		assertEquals("Update count should be 1, is: " + updateCount[0], 1, updateCount[0]);
	}
}
