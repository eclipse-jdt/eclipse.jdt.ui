/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.junit.tests;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.junit.launcher.ITestKind;
import org.eclipse.jdt.internal.junit.launcher.JUnit4TestFinder;
import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;
import org.eclipse.jdt.internal.junit.util.TestSearchEngine;

import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;


public class TestTestSearchEngine extends TestCase {
	private IJavaProject fProject;
	private IPackageFragmentRoot fRoot;

	protected void setUp() throws Exception {
		super.setUp();
		fProject= JavaProjectHelper.createJavaProject("TestProject", "bin");
		JavaProjectHelper.addRTJar(fProject);
		JavaProjectHelper.addVariableEntry(fProject, new Path(
			"JUNIT_HOME/junit.jar"), null, null);
		fRoot= JavaProjectHelper.addSourceContainer(fProject, "src");
	}

	protected void tearDown() throws Exception {
		JavaProjectHelper.delete(fProject);
		super.tearDown();
	}


	public void testOnePackage() throws Exception {
		IPackageFragment p= fRoot.createPackageFragment("p", true, null);
		ICompilationUnit test1= createCompilationUnit(p, 1);
		ICompilationUnit test2= createCompilationUnit(p, 2);

		IType[] result= findTests(p);
		assertEqualTypes("Test case not found", new IType[] {
				test1.getType("Test1"), test2.getType("Test2")
			}, result);
	}

	public void testTwoPackages() throws Exception {
		IPackageFragment p= fRoot.createPackageFragment("p", true, null);
		ICompilationUnit test1= createCompilationUnit(p, 1);
		ICompilationUnit test2= createCompilationUnit(p, 2);

		IPackageFragment q= fRoot.createPackageFragment("q", true, null);
		ICompilationUnit test3= createCompilationUnit(q, 3);

		IType[] result= findTests(new IJavaElement[] {p, q});
		assertEqualTypes("Test case not found", new IType[] {
				test1.getType("Test1"), test2.getType("Test2"), test3.getType("Test3")
			}, result);
	}

	public void testTwoPackagesSearchingInOne() throws Exception {
		IPackageFragment p= fRoot.createPackageFragment("p", true, null);
		ICompilationUnit test1= createCompilationUnit(p, 1);
		ICompilationUnit test2= createCompilationUnit(p, 2);

		IPackageFragment q= fRoot.createPackageFragment("q", true, null);
		createCompilationUnit(q, 3);

		IType[] result= findTests(p);
		assertEqualTypes("Test case not found", new IType[] {
				test1.getType("Test1"), test2.getType("Test2")
			}, result);
	}

	public void testPackageFragmentRoot() throws Exception {
		IPackageFragment p= fRoot.createPackageFragment("p", true, null);
		ICompilationUnit test1= createCompilationUnit(p, 1);
		ICompilationUnit test2= createCompilationUnit(p, 2);

		IPackageFragment q= fRoot.createPackageFragment("q", true, null);
		ICompilationUnit test3= createCompilationUnit(q, 3);

		IType[] result= findTests(fRoot);
		assertEqualTypes("Test case not found", new IType[] {
				test1.getType("Test1"), test2.getType("Test2"), test3.getType("Test3")
			}, result);
	}

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

		IType[] result= findTests(new IJavaElement[] {fRoot, root2});
		assertEqualTypes("Test case not found", new IType[] {
				test1.getType("Test1"), test2.getType("Test2"), test3.getType("Test3"),
				test4.getType("Test4"), test5.getType("Test5")
			}, result);
	}

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

		IType[] result= findTests(root2);
		assertEqualTypes("Test case not found", new IType[] {
				test4.getType("Test4"), test5.getType("Test5")
			}, result);
	}

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

		IType[] result= findTests(root2);
		assertEqualTypes("Test case not found", new IType[] { testSub.getType("TestSub") }, result);
	}

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

		IType[] result= findTests(fProject);
		assertEqualTypes("Test case not found", new IType[] {
				test1.getType("Test1"), test2.getType("Test2"), test3.getType("Test3"),
				test4.getType("Test4"), test5.getType("Test5")
			}, result);
	}

	public void testSubPackage() throws Exception {
		IPackageFragment p= fRoot.createPackageFragment("p", true, null);
		ICompilationUnit test1= createCompilationUnit(p, 1);

		IPackageFragment q= fRoot.createPackageFragment("p.q", true, null);
		createCompilationUnit(q, 2);

		IType[] result= findTests(p);
		assertEqualTypes("Test case not found", new IType[] {
				test1.getType("Test1")
			}, result);
	}

	private IType[] findTests(IJavaElement element) throws InvocationTargetException, InterruptedException {
		ITestKind testKind= TestKindRegistry.getContainerTestKind(fProject);
		return TestSearchEngine.findTests(new BusyIndicatorRunnableContext(), element, testKind);
	}

	private IType[] findTests(IJavaElement[] elements) throws InvocationTargetException, InterruptedException {
		HashSet res= new HashSet();
		for (int i= 0; i < elements.length; i++) {
			IType[] types= findTests(elements[i]);
			for (int k= 0; k < types.length; k++) {
				res.add(types[k]);
			}
		}
		return (IType[]) res.toArray(new IType[res.size()]);
	}


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

	private void assertEqualTypes(String message, IType[] expected, IType[] actual) {
		assertEquals("Wrong number of found tests", expected.length, actual.length);
		List list= Arrays.asList(expected);
		for (int i= 0; i < actual.length; i++) {
			if (!list.contains(expected[i])) {
				fail(message + expected[i].getFullyQualifiedName());
			}
		}
	}
}
