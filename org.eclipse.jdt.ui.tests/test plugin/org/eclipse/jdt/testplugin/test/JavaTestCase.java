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
package org.eclipse.jdt.testplugin.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;

public class JavaTestCase  {
	private IJavaProject fJavaProject;

	/**
	 * Creates a new test Java project.
	 *
	 * @throws Exception in case of any problem
	 */
	@BeforeEach
	public void setUp() throws Exception {
		fJavaProject= JavaProjectHelper.createJavaProject("HelloWorldProject", "bin");

		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fJavaProject, "src");
		IPackageFragment pack= root.createPackageFragment("ibm.util", true, null);

		ICompilationUnit cu= pack.getCompilationUnit("A.java");
		IType type= cu.createType("public class A {\n}\n", null, true, null);
		type.createMethod("public void a() {}\n", null, true, null);
		type.createMethod("public void b(java.util.Vector v) {}\n", null, true, null);
	}

	/**
	 * Removes the test java project.
	 *
	 * @throws Exception in case of any problem
	 */
	@AfterEach
	public void tearDown () throws Exception {
		JavaProjectHelper.delete(fJavaProject);
	}

	/*
	 * Basic test: Checks for created methods.
	 */
	@Test
	public void doTest1() throws Exception {
		String name= "ibm/util/A.java";
		ICompilationUnit cu= (ICompilationUnit) fJavaProject.findElement(new Path(name));
		assertNotNull(cu, "A.java must exist");
		IType type= cu.getType("A");
		assertNotNull(type, "Type A must exist");

		System.out.println("methods of A");
		IMethod[] methods= type.getMethods();
		for (IMethod method : methods) {
			System.out.println(method.getElementName());
		}
		assertEquals(2, methods.length, "Should contain 2 methods");
	}
}
