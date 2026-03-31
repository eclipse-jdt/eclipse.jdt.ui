/*******************************************************************************
 * Copyright (c) 2000, 2026 IBM Corporation and others.
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

import static org.assertj.core.api.Assertions.assertThat;

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

	//--------------------------------------------------------------------------
	// Tests for TestSearchEngine.findTests:

	@Test
	public void testOnePackage() throws Exception {
		IPackageFragment p= fRoot.createPackageFragment("p", true, null);
		ICompilationUnit test1= createCompilationUnit(p, 1);
		ICompilationUnit test2= createCompilationUnit(p, 2);

		var result= findTests(p);
		assertThat(result).containsExactlyInAnyOrder(
				test1.getType("Test1"), test2.getType("Test2")
		);
	}

	@Test
	public void testTwoPackages() throws Exception {
		IPackageFragment p= fRoot.createPackageFragment("p", true, null);
		ICompilationUnit test1= createCompilationUnit(p, 1);
		ICompilationUnit test2= createCompilationUnit(p, 2);

		IPackageFragment q= fRoot.createPackageFragment("q", true, null);
		ICompilationUnit test3= createCompilationUnit(q, 3);

		var result= findTests(new IJavaElement[] {p, q});
		assertThat(result).containsExactlyInAnyOrder(
				test1.getType("Test1"), test2.getType("Test2"), test3.getType("Test3")
		);
	}

	@Test
	public void testTwoPackagesSearchingInOne() throws Exception {
		IPackageFragment p= fRoot.createPackageFragment("p", true, null);
		ICompilationUnit test1= createCompilationUnit(p, 1);
		ICompilationUnit test2= createCompilationUnit(p, 2);

		IPackageFragment q= fRoot.createPackageFragment("q", true, null);
		createCompilationUnit(q, 3);

		var result= findTests(p);
		assertThat(result).containsExactlyInAnyOrder(
				test1.getType("Test1"), test2.getType("Test2")
		);
	}

	@Test
	public void testPackageFragmentRoot() throws Exception {
		IPackageFragment p= fRoot.createPackageFragment("p", true, null);
		ICompilationUnit test1= createCompilationUnit(p, 1);
		ICompilationUnit test2= createCompilationUnit(p, 2);

		IPackageFragment q= fRoot.createPackageFragment("q", true, null);
		ICompilationUnit test3= createCompilationUnit(q, 3);

		var result= findTests(fRoot);
		assertThat(result).containsExactlyInAnyOrder(
				test1.getType("Test1"), test2.getType("Test2"), test3.getType("Test3")
		);
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
		assertThat(result).containsExactlyInAnyOrder(
				test1.getType("Test1"), test2.getType("Test2"), test3.getType("Test3"),
				test4.getType("Test4"), test5.getType("Test5")
		);
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
		assertThat(result).containsExactlyInAnyOrder(
				test4.getType("Test4"), test5.getType("Test5")
		);
	}

	@Test // for https://bugs.eclipse.org/bugs/show_bug.cgi?id=139961
	public void testTwoPackageFragmentRootsSearchingInOneNoSupertype() throws Exception {
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
		assertThat(result).containsExactlyInAnyOrder(testSub.getType("TestSub"));
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
		assertThat(result).containsExactlyInAnyOrder(
				test1.getType("Test1"), test2.getType("Test2"), test3.getType("Test3"),
				test4.getType("Test4"), test5.getType("Test5")
		);
	}

	@Test
	public void testSubPackage() throws Exception {
		IPackageFragment p= fRoot.createPackageFragment("p", true, null);
		ICompilationUnit test1= createCompilationUnit(p, 1);

		IPackageFragment q= fRoot.createPackageFragment("p.q", true, null);
		createCompilationUnit(q, 2);

		var result= findTests(p);
		assertThat(result).containsExactlyInAnyOrder(test1.getType("Test1"));
	}

	//

	private List<IType> findTests(IJavaElement element) throws InvocationTargetException, InterruptedException {
		ITestKind testKind= TestKindRegistry.getContainerTestKind(fProject);
		return new ArrayList<>(TestSearchEngine.findTests(new BusyIndicatorRunnableContext(), element, testKind));
	}

	private List<IType> findTests(IJavaElement[] elements) throws InvocationTargetException, InterruptedException {
		var set= new HashSet<IType>();
		for (IJavaElement element : elements) {
			var types= findTests(element);
			set.addAll(types);
		}
		return new ArrayList<>(set);
	}

	private ICompilationUnit createCompilationUnit(IPackageFragment pack, int number) throws JavaModelException {
		return pack.createCompilationUnit(
			"Test"+number+".java",
			"package " + pack.getElementName() + "; import junit.framework.TestCase; public class Test" + number + " extends TestCase { }",
			true, null);
	}

	//--------------------------------------------------------------------------
	// Tests for TestSearchEngine.findTests:

	@Test
	public void testNoMethods() throws Exception {
		IPackageFragment pack= fRoot.createPackageFragment("p", true, null);
		ICompilationUnit unit= pack.createCompilationUnit("SomeTest.java",
				"""
				package p;
				import org.junit.*;
				public class SomeTest {
				}
				""", true, null);

		var names = findTestMethods(unit.findPrimaryType());

		assertThat(names).isEmpty();
	}

	@Test
	public void testOneMethod() throws Exception {
		IPackageFragment pack= fRoot.createPackageFragment("p", true, null);
		ICompilationUnit unit= pack.createCompilationUnit("SomeTest.java",
				"""
				package p;
				import org.junit.*;
				public class SomeTest {
					@Test public void someTest() {}
				}
				""", true, null);

		var names = findTestMethods(unit.findPrimaryType());

		assertThat(names).containsExactlyInAnyOrder("someTest");
	}

	@Test
	public void testTwoMethods() throws Exception {
		IPackageFragment pack= fRoot.createPackageFragment("p", true, null);
		ICompilationUnit unit= pack.createCompilationUnit("SomeTest.java",
				"""
				package p; import org.junit.*;
				public class SomeTest {
					static  void testOne() {}
					private void testTwo() {}
					public  void testThree() {}
					@Test public void four() {}
				}
				""", true, null);

		var names = findTestMethods(unit.findPrimaryType());

		assertThat(names).containsExactlyInAnyOrder("four", "testThree");
	}

	@Test
	public void testSuperMethods() throws Exception {
		IPackageFragment pack= fRoot.createPackageFragment("p", true, null);
		pack.createCompilationUnit("A.java",
				"""
				package p;
				import org.junit.*;
				public interface A {
					@Test default void a() {}
				}
				""", true, null);
		pack.createCompilationUnit("B.java",
				"""
				package p;
				import org.junit.*;
					public abstract class B {
					public abstract void x();
					@Test void b() {}
				}"
				""", true, null);
		ICompilationUnit unit= pack.createCompilationUnit("C.java",
				"""
				package p;
				public class C extends B implements A {
					@Override public void x() {}
				}
				""", true, null);

		var names = findTestMethods(unit.findPrimaryType());

		assertThat(names).containsExactlyInAnyOrder("a", "b");
	}

	@Test
	public void testRejectMethods() throws Exception {
		IPackageFragment pack= fRoot.createPackageFragment("p", true, null);
		ICompilationUnit unit= pack.createCompilationUnit("C.java",
				"""
				package p;
				import org.junit.*;"
				public class C {
					public void tester(Object o) {}
					public int tests() {return 42;}
				}
				""", true, null);

		var names = findTestMethods(unit.findPrimaryType());

		assertThat(names).isEmpty();
	}

	@Test
	public void testLegacyMethods() throws Exception {
		JavaProjectHelper.removeFromClasspath(fProject, JUnitCore.JUNIT4_CONTAINER_PATH);
		JavaProjectHelper.addToClasspath(fProject, JavaCore.newContainerEntry(JUnitCore.JUNIT3_CONTAINER_PATH));

		IPackageFragment pack= fRoot.createPackageFragment("p", true, null);
		ICompilationUnit unit= pack.createCompilationUnit("C.java",
				"""
				package p;
				public class C extends junit.framework.TestCase {
					public void testFoo() {}
					public void testBar() {}
					public void testBaz() {}
				}
				""", true, null);

		var names = findTestMethods(unit.findPrimaryType());

		assertThat(names).containsExactlyInAnyOrder("testFoo", "testBar", "testBaz");
	}

	@Test
	public void testJupiterMethods() throws Exception {
		JavaProjectHelper.removeFromClasspath(fProject, JUnitCore.JUNIT4_CONTAINER_PATH);
		JavaProjectHelper.addToClasspath(fProject, JavaCore.newContainerEntry(JUnitCore.JUNIT5_CONTAINER_PATH));

		IPackageFragment pack= fRoot.createPackageFragment("p", true, null);
		ICompilationUnit unit= pack.createCompilationUnit("C.java",
				"""
				package p;
				import java.util.*;
				import org.junit.jupiter.api.*;
				import org.junit.jupiter.params.*;
				import org.junit.jupiter.params.provider.*;
				import org.junit.platform.commons.annotation.*;
				import static org.junit.jupiter.api.DynamicTest.*;
				class C {
					void nope() {}
					@Test void foo() {}
					@Testable void bar() {}
					@TestTemplate void baz(int i) {}
					@ParameterizedTest @CsvSource("!") void boz(String s) {}
					@RepeatedTest(2) void foofoo(RepetitionInfo repsInfo) {}
					@TestFactory Collection<DynamicTest> fizzBuzz() {
						return List.of(
							dynamicTest("fizz", () -> {}),
							dynamicTest("buzz", () -> {}));
					}
				}
				""", true, null);

		var names = findTestMethods(unit.findPrimaryType());

		assertThat(names).containsExactlyInAnyOrder(
				"foo", "bar", "baz(int)", "boz(java.lang.String)",
				"foofoo(org.junit.jupiter.api.RepetitionInfo)", "fizzBuzz");
	}

	//

	private List<String> findTestMethods(IType type) throws InvocationTargetException, InterruptedException {
		var testKind= (org.eclipse.jdt.internal.junit.launcher.TestKind) TestKindRegistry.getContainerTestKind(fProject);
		return new ArrayList<>(TestSearchEngine.findTestMethods(new BusyIndicatorRunnableContext(), fProject, type, testKind));
	}

	//--------------------------------------------------------------------------

	@Test // for https://bugs.eclipse.org/bugs/show_bug.cgi?id=151003
	public void testJUnit4NoSrc() throws Exception {
		IType noTest= fProject.findType("java.lang.Integer");
		assertThat(new JUnit4TestFinder().isTest(noTest)).isFalse();
	}
}
