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
package org.eclipse.jdt.junit.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.junit.launcher.ITestKind;
import org.eclipse.jdt.internal.junit.launcher.JUnit4TestFinder;
import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;
import org.eclipse.jdt.internal.junit.util.TestSearchEngine;

import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;


public class TestTestSearchEngine {
	private IJavaProject fProject;
	private IPackageFragmentRoot fRoot;

	@Before
	public void setUp() throws Exception {
		fProject= JavaProjectHelper.createJavaProject("TestProject", "bin");
		JavaProjectHelper.addRTJar(fProject);
		JavaProjectHelper.addToClasspath(fProject, JavaCore.newContainerEntry(JUnitCore.JUNIT4_CONTAINER_PATH));
		fRoot= JavaProjectHelper.addSourceContainer(fProject, "src");
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.delete(fProject);
	}

	@Test
	public void testOnePackage() throws Exception {
		IPackageFragment p= fRoot.createPackageFragment("p", true, null);
		ICompilationUnit test1= createCompilationUnit(p, 1);
		ICompilationUnit test2= createCompilationUnit(p, 2);

		var result= findTests(p);
		assertEqualTypes("Test case not found", List.of(
				test1.getType("Test1"), test2.getType("Test2")
			), result);
	}

	@Test
	public void testTwoPackages() throws Exception {
		IPackageFragment p= fRoot.createPackageFragment("p", true, null);
		ICompilationUnit test1= createCompilationUnit(p, 1);
		ICompilationUnit test2= createCompilationUnit(p, 2);

		IPackageFragment q= fRoot.createPackageFragment("q", true, null);
		ICompilationUnit test3= createCompilationUnit(q, 3);

		var result= findTests(new IJavaElement[] {p, q});
		assertEqualTypes("Test case not found", List.of(
				test1.getType("Test1"), test2.getType("Test2"), test3.getType("Test3")
			), result);
	}

	@Test
	public void testTwoPackagesSearchingInOne() throws Exception {
		IPackageFragment p= fRoot.createPackageFragment("p", true, null);
		ICompilationUnit test1= createCompilationUnit(p, 1);
		ICompilationUnit test2= createCompilationUnit(p, 2);

		IPackageFragment q= fRoot.createPackageFragment("q", true, null);
		createCompilationUnit(q, 3);

		var result= findTests(p);
		assertEqualTypes("Test case not found", List.of(
				test1.getType("Test1"), test2.getType("Test2")
			), result);
	}

	@Test
	public void testPackageFragmentRoot() throws Exception {
		IPackageFragment p= fRoot.createPackageFragment("p", true, null);
		ICompilationUnit test1= createCompilationUnit(p, 1);
		ICompilationUnit test2= createCompilationUnit(p, 2);

		IPackageFragment q= fRoot.createPackageFragment("q", true, null);
		ICompilationUnit test3= createCompilationUnit(q, 3);

		var result= findTests(fRoot);
		assertEqualTypes("Test case not found", List.of(
				test1.getType("Test1"), test2.getType("Test2"), test3.getType("Test3")
			), result);
	}

	@Test
	public void testTwoPackageFragmentRoots() throws Exception {
		IPackageFragment p= fRoot.createPackageFragment("p", true, null);
		ICompilationUnit test1= createCompilationUnit(p, 1);
		ICompilationUnit test2= createCompilationUnit(p, 2);

		IPackageFragment q= fRoot.createPackageFragment("q", true, null);
		ICompilationUnit test3= createCompilationUnit(q, 3);

		IPackageFragmentRoot root2= JavaProjectHelper.addSourceContainer(fProject, "tests");

		IPackageFragment r= root2.createPackageFragment("r", true, null);
		ICompilationUnit test4= createCompilationUnit(r, 4);
		ICompilationUnit test5= createCompilationUnit(r, 5);

		var result= findTests(new IJavaElement[] {fRoot, root2});
		assertEqualTypes("Test case not found", List.of(
				test1.getType("Test1"), test2.getType("Test2"), test3.getType("Test3"),
				test4.getType("Test4"), test5.getType("Test5")
			), result);
	}

	@Test
	public void testTwoPackageFragmentRootsSearchingInOne() throws Exception {
		IPackageFragment p= fRoot.createPackageFragment("p", true, null);
		createCompilationUnit(p, 1);
		createCompilationUnit(p, 2);

		IPackageFragment q= fRoot.createPackageFragment("q", true, null);
		createCompilationUnit(q, 3);

		IPackageFragmentRoot root2= JavaProjectHelper.addSourceContainer(fProject, "tests");

		IPackageFragment r= root2.createPackageFragment("r", true, null);
		ICompilationUnit test4= createCompilationUnit(r, 4);
		ICompilationUnit test5= createCompilationUnit(r, 5);

		var result= findTests(root2);
		assertEqualTypes("Test case not found", List.of(
				test4.getType("Test4"), test5.getType("Test5")
			), result);
	}

	@Test
	public void testTwoPackageFragmentRootsSearchingInOneNoSupertype() throws Exception {
		// regression test for https://bugs.eclipse.org/bugs/show_bug.cgi?id=139961
		IPackageFragment p= fRoot.createPackageFragment("p", true, null);
		createCompilationUnit(p, 1);

		IPackageFragmentRoot root2= JavaProjectHelper.addSourceContainer(fProject, "tests");
		IPackageFragment r= root2.createPackageFragment("r", true, null);
		ICompilationUnit testSub= r.createCompilationUnit(
				"TestSub.java",
				"package r; import p.Test1; public class TestSub extends Test1 { }",
				true,
				null);

		var result= findTests(root2);
		assertEqualTypes("Test case not found", List.of(testSub.getType("TestSub")), result);
	}

	@Test
	public void testProject() throws Exception {
		IPackageFragment p= fRoot.createPackageFragment("p", true, null);
		ICompilationUnit test1= createCompilationUnit(p, 1);
		ICompilationUnit test2= createCompilationUnit(p, 2);

		IPackageFragment q= fRoot.createPackageFragment("q", true, null);
		ICompilationUnit test3= createCompilationUnit(q, 3);

		IPackageFragmentRoot root2= JavaProjectHelper.addSourceContainer(fProject, "tests");

		IPackageFragment r= root2.createPackageFragment("r", true, null);
		ICompilationUnit test4= createCompilationUnit(r, 4);
		ICompilationUnit test5= createCompilationUnit(r, 5);

		var result= findTests(fProject);
		assertEqualTypes("Test case not found", List.of(
				test1.getType("Test1"), test2.getType("Test2"), test3.getType("Test3"),
				test4.getType("Test4"), test5.getType("Test5")
			), result);
	}

	@Test
	public void testSubPackage() throws Exception {
		IPackageFragment p= fRoot.createPackageFragment("p", true, null);
		ICompilationUnit test1= createCompilationUnit(p, 1);

		IPackageFragment q= fRoot.createPackageFragment("p.q", true, null);
		createCompilationUnit(q, 2);

		var result= findTests(p);
		assertEqualTypes("Test case not found", List.of(test1.getType("Test1")), result);
	}

	private List<IType> findTests(IJavaElement element) throws InvocationTargetException, InterruptedException {
		ITestKind testKind= TestKindRegistry.getContainerTestKind(fProject);
		return new ArrayList<>(TestSearchEngine.findTests(new BusyIndicatorRunnableContext(), element, testKind));
	}

	private List<IType> findTests(IJavaElement[] elements) throws InvocationTargetException, InterruptedException {
		HashSet<IType> res= new HashSet<>();
		for (IJavaElement element : elements) {
			var types= findTests(element);
			res.addAll(types);
		}
		return new ArrayList<>(res);
	}

	@Test
	public void testJUnit4NoSrc() throws Exception {
		//regression test for https://bugs.eclipse.org/bugs/show_bug.cgi?id=151003
		IType noTest= fProject.findType("java.lang.Integer");
		assertFalse(new JUnit4TestFinder().isTest(noTest));
	}

	private ICompilationUnit createCompilationUnit(IPackageFragment pack, int number) throws JavaModelException {
		return pack.createCompilationUnit(
			"Test"+number+".java",
			"package " + pack.getElementName() + "; import junit.framework.TestCase; public class Test" + number + " extends TestCase { }",
			true, null);
	}

	private void assertEqualTypes(String message, List<IType> expected, List<IType> actual) {
		assertEquals("Wrong number of found tests", expected.size(), actual.size());
		for (int i= 0; i < actual.size(); i++) {
			assertTrue(message + expected.get(i).getFullyQualifiedName(), expected.contains(actual.get(i)));
		}
	}
}
