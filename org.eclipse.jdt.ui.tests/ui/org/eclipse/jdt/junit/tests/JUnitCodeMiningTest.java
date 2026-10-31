/*******************************************************************************
 * Copyright (c) 2017, 2026 Angelo Zerr and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation,
 *         originally in https://github.com/angelozerr/jdt-codemining (EPL)
 *     Kai Winter <kaiwinter@gmx.de> - JUnit 5 ParameterizedTest support in jdt-codemining
 *******************************************************************************/
package org.eclipse.jdt.junit.tests;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.Test;

import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.junit.model.ITestElement.Result;
import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.resources.IResource;

import org.eclipse.core.runtime.IPath;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;
import org.eclipse.jdt.internal.junit.model.TestCaseElement;
import org.eclipse.jdt.internal.junit.model.TestElement.Status;
import org.eclipse.jdt.internal.junit.model.TestRunSession;
import org.eclipse.jdt.internal.junit.model.TestSuiteElement;
import org.eclipse.jdt.internal.junit.ui.codemining.JUnitStatusRegistry;
import org.eclipse.jdt.internal.junit.util.TestSearchEngine;

/**
 * Tests the logic behind the JUnit code minings: the detection of test methods for the JUnit
 * versions, the registry of test results and the preferences enabling the code minings.
 */
public class JUnitCodeMiningTest {

	private IJavaProject fProject;

	@After
	public void tearDown() throws Exception {
		if (fProject != null) {
			JavaProjectHelper.delete(fProject);
		}
	}

	private IType createType(IPath container, String testKindId, String source) throws Exception {
		fProject= JavaProjectHelper.createJavaProject("CodeMiningProject", "bin");
		JavaProjectHelper.addRTJar(fProject);
		JavaProjectHelper.addToClasspath(fProject, JavaCore.newContainerEntry(container));
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fProject, "src");
		ICompilationUnit cu= root.createPackageFragment("p", true, null).createCompilationUnit("MyTest.java", source, false, null);
		assertThat(TestKindRegistry.getContainerTestKindId(cu)).isEqualTo(testKindId);
		return cu.findPrimaryType();
	}

	private static Result findResult(JUnitStatusRegistry registry, IMethod method) throws Exception {
		return registry.getResults(method.getDeclaringType()).findResult(method);
	}

	private static Result findResult(JUnitStatusRegistry registry, IType type) throws Exception {
		return registry.getResults(type).findTypeResult(null);
	}

	private boolean isTest(IType type, String methodName, String testKindId) throws Exception {
		IMethod method= type.getMethod(methodName, new String[0]);
		assertThat(method.exists()).isTrue();
		return TestSearchEngine.isTestMethod(method, fProject, testKindId, TestSearchEngine.isConcreteTestImplementor(type));
	}

	@Test
	public void testJUnit3Methods() throws Exception {
		IType type= createType(JUnitCore.JUNIT3_CONTAINER_PATH, TestKindRegistry.JUNIT3_TEST_KIND_ID, """
				package p;
				import junit.framework.TestCase;
				public class MyTest extends TestCase {
					public void testOk() {}
					public void notATest() {}
					private void testPrivate() {}
					public static void testStatic() {}
					public int testNotVoid() { return 0; }
					public void testWithArgument(int i) {}
				}
				""");
		String kind= TestKindRegistry.JUNIT3_TEST_KIND_ID;
		assertThat(isTest(type, "testOk", kind)).isTrue();
		assertThat(isTest(type, "notATest", kind)).isFalse();
		assertThat(isTest(type, "testPrivate", kind)).isFalse();
		assertThat(isTest(type, "testStatic", kind)).isFalse();
		assertThat(isTest(type, "testNotVoid", kind)).isFalse();
		assertThat(type.getMethod("testWithArgument", new String[] { "I" }).exists()).isTrue();
		assertThat(TestSearchEngine.isTestMethod(type.getMethod("testWithArgument", new String[] { "I" }), fProject, kind, true)).isFalse();
	}

	@Test
	public void testJUnit4Methods() throws Exception {
		IType type= createType(JUnitCore.JUNIT4_CONTAINER_PATH, TestKindRegistry.JUNIT4_TEST_KIND_ID, """
				package p;
				import org.junit.Test;
				public class MyTest {
					@Test public void annotated() {}
					@org.junit.Test public void qualified() {}
					@org.junit.Test void qualifiedPackagePrivate() {}
					@Test void packagePrivateAnnotated() {}
					@Test public int notVoid() { return 0; }
					@Test public void withArgument(int i) {}
					@Test private void privateAnnotated() {}
					@Test public static void staticAnnotated() {}
					public void notAnnotated() {}
				}
				""");
		String kind= TestKindRegistry.JUNIT4_TEST_KIND_ID;
		assertThat(isTest(type, "annotated", kind)).isTrue();
		assertThat(isTest(type, "qualified", kind)).isTrue();
		assertThat(isTest(type, "qualifiedPackagePrivate", kind)).isFalse();
		assertThat(isTest(type, "packagePrivateAnnotated", kind)).isFalse(); // JUnit 4 needs public
		assertThat(isTest(type, "notVoid", kind)).isFalse();
		assertThat(TestSearchEngine.isTestMethod(type.getMethod("withArgument", new String[] { "I" }), fProject, kind, false)).isFalse();
		assertThat(isTest(type, "privateAnnotated", kind)).isFalse();
		assertThat(isTest(type, "staticAnnotated", kind)).isFalse();
		assertThat(isTest(type, "notAnnotated", kind)).isFalse();
	}

	@Test
	public void testJUnit5Methods() throws Exception {
		IType type= createType(JUnitCore.JUNIT5_CONTAINER_PATH, TestKindRegistry.JUNIT5_TEST_KIND_ID, """
				package p;
				import org.junit.jupiter.api.Test;
				import org.junit.jupiter.api.RepeatedTest;
				import org.junit.jupiter.params.ParameterizedTest;
				public class MyTest {
					@Test void plain() {}
					public void testWithoutAnnotation() {}
					@RepeatedTest(2) void repeated() {}
					@ParameterizedTest void parameterized() {}
					@Test private void privateAnnotated() {}
					@Test int notVoid() { return 0; }
					void notAnnotated() {}
				}
				""");
		String kind= TestKindRegistry.JUNIT5_TEST_KIND_ID;
		assertThat(isTest(type, "plain", kind)).isTrue();
		assertThat(isTest(type, "repeated", kind)).isTrue();
		assertThat(isTest(type, "parameterized", kind)).isTrue();
		assertThat(isTest(type, "privateAnnotated", kind)).isFalse();
		assertThat(isTest(type, "notVoid", kind)).isFalse();
		assertThat(isTest(type, "notAnnotated", kind)).isFalse();
		assertThat(isTest(type, "testWithoutAnnotation", kind)).isFalse();
	}

	@Test
	public void testAnotherAnnotationNamedTestIsNotATest() throws Exception {
		IType type= createType(JUnitCore.JUNIT4_CONTAINER_PATH, TestKindRegistry.JUNIT4_TEST_KIND_ID, """
				package p;
				public class MyTest {
					@Test public void a() {}
				}
				@interface Test {}
				""");
		assertThat(isTest(type, "a", TestKindRegistry.JUNIT4_TEST_KIND_ID)).isFalse();
	}

	@Test
	public void testHelperClassIsNotATest() throws Exception {
		IType type= createType(JUnitCore.JUNIT4_CONTAINER_PATH, TestKindRegistry.JUNIT4_TEST_KIND_ID, """
				package p;
				public class MyTest {
					public void testHelper() {}
				}
				""");
		assertThat(isTest(type, "testHelper", TestKindRegistry.JUNIT4_TEST_KIND_ID)).isFalse();
		assertThat(isTest(type, "testHelper", null)).isFalse();
	}

	@Test
	public void testStatusRegistry() throws Exception {
		IType type= createType(JUnitCore.JUNIT4_CONTAINER_PATH, TestKindRegistry.JUNIT4_TEST_KIND_ID, """
				package p;
				import org.junit.Test;
				public class MyTest {
					@Test public void a() {}
					@Test public void b() {}
				}
				""");
		IMethod a= type.getMethod("a", new String[0]);
		IMethod b= type.getMethod("b", new String[0]);
		String project= fProject.getElementName();
		JUnitStatusRegistry registry= new JUnitStatusRegistry();
		assertThat(findResult(registry, a)).isNull();
		assertThat(findResult(registry, type)).isNull();

		registry.record(1, project, "p.MyTest", "a", null, Result.OK);
		assertThat(findResult(registry, a)).isEqualTo(Result.OK);
		assertThat(findResult(registry, b)).isNull();
		assertThat(findResult(registry, type)).isEqualTo(Result.OK);

		registry.record(1, project, "p.MyTest", "b", null, Result.FAILURE);
		assertThat(findResult(registry, type)).isEqualTo(Result.FAILURE);

		// the first result of a later run replaces the result of the previous run
		registry.record(2, project, "p.MyTest", "b", null, Result.OK);
		assertThat(findResult(registry, b)).isEqualTo(Result.OK);
		assertThat(findResult(registry, type)).isEqualTo(Result.OK);
	}

	@Test
	public void testSeveralResultsOfOneMethodInOneRun() throws Exception {
		IType type= createType(JUnitCore.JUNIT5_CONTAINER_PATH, TestKindRegistry.JUNIT5_TEST_KIND_ID, """
				package p;
				import org.junit.jupiter.params.ParameterizedTest;
				public class MyTest {
					@ParameterizedTest void a(String s) {}
				}
				""");
		IMethod a= type.getMethod("a", new String[] { "QString;" });
		String project= fProject.getElementName();
		JUnitStatusRegistry registry= new JUnitStatusRegistry();
		registry.record(1, project, "p.MyTest", "a", new String[] { "java.lang.String" }, Result.FAILURE);
		registry.record(1, project, "p.MyTest", "a", new String[] { "java.lang.String" }, Result.OK);
		registry.record(1, project, "p.MyTest", "a", new String[] { "java.lang.String" }, Result.OK);
		assertThat(findResult(registry, a)).isEqualTo(Result.FAILURE);
		registry.record(2, project, "p.MyTest", "a", new String[] { "java.lang.String" }, Result.OK);
		assertThat(findResult(registry, a)).isEqualTo(Result.OK);
	}

	@Test
	public void testResultsOfMissingMethodsAreIgnored() throws Exception {
		IType type= createType(JUnitCore.JUNIT4_CONTAINER_PATH, TestKindRegistry.JUNIT4_TEST_KIND_ID, """
				package p;
				import org.junit.Test;
				public class MyTest {
					@Test public void a() {}
				}
				""");
		String project= fProject.getElementName();
		JUnitStatusRegistry registry= new JUnitStatusRegistry();
		registry.record(1, project, "p.MyTest", "deleted", null, Result.FAILURE);
		assertThat(findResult(registry, type)).isNull();
		registry.record(1, project, "p.MyTest", "a", null, Result.OK);
		assertThat(findResult(registry, type)).isEqualTo(Result.OK);
	}

	@Test
	public void testResultsOfDeletedProjectAreForgotten() throws Exception {
		IType type= createType(JUnitCore.JUNIT4_CONTAINER_PATH, TestKindRegistry.JUNIT4_TEST_KIND_ID, """
				package p;
				import org.junit.Test;
				public class MyTest {
					@Test public void a() {}
				}
				""");
		String project= fProject.getElementName();
		JUnitStatusRegistry registry= new JUnitStatusRegistry();
		registry.start();
		try {
			registry.record(1, project, "p.MyTest", "a", null, Result.FAILURE);
			assertThat(findResult(registry, type)).isEqualTo(Result.FAILURE);
			JavaProjectHelper.delete(fProject);
			// same project name again
			type= createType(JUnitCore.JUNIT4_CONTAINER_PATH, TestKindRegistry.JUNIT4_TEST_KIND_ID, """
					package p;
					import org.junit.Test;
					public class MyTest {
						@Test public void a() {}
					}
					""");
			assertThat(findResult(registry, type)).isNull();
		} finally {
			registry.dispose();
		}
	}

	@Test
	public void testUniqueIdWithParenthesesInDisplayName() {
		// the display name of a class or method is not part of the unique id
		var reference= JUnitStatusRegistry.parseUniqueId("[engine:junit-jupiter]/[class:p.Outer]/[nested-class:Inner]/[method:plain()]");
		assertThat(reference.className()).isEqualTo("p.Outer$Inner");
		assertThat(reference.methodName()).isEqualTo("plain");
		assertThat(reference.parameterTypes()).isEmpty();

		reference= JUnitStatusRegistry.parseUniqueId("[engine:junit-jupiter]/[class:p.MyTest]/[test-template:a(java.lang.String, int)]/[test-template-invocation:#2]");
		assertThat(reference.className()).isEqualTo("p.MyTest");
		assertThat(reference.methodName()).isEqualTo("a");
		assertThat(reference.parameterTypes()).containsExactly("java.lang.String", "int");

		reference= JUnitStatusRegistry.parseUniqueId("[engine:junit-jupiter]/[class:p.MyTest]/[test-factory:f()]/[dynamic-container:#1]/[dynamic-test:#1]");
		assertThat(reference.methodName()).isEqualTo("f");

		reference= JUnitStatusRegistry.parseUniqueId("[engine:junit-jupiter]/[class:p.My$Test]/[method:plain()]");
		assertThat(reference.className()).isEqualTo("p.My$Test");

		assertThat(JUnitStatusRegistry.parseUniqueId(null)).isNull();
		assertThat(JUnitStatusRegistry.parseUniqueId("[engine:junit-vintage]/[runner:p.MyTest]/[test:a(p.MyTest)]")).isNull();
	}

	@Test
	public void testInheritedTestsAreInTheStatusOfTheSubclass() throws Exception {
		IType base= createType(JUnitCore.JUNIT4_CONTAINER_PATH, TestKindRegistry.JUNIT4_TEST_KIND_ID, """
				package p;
				import org.junit.Test;
				public abstract class MyTest {
					@Test public void a() {}
				}
				""");
		IType sub= base.getPackageFragment().createCompilationUnit("Sub.java", """
				package p;
				public class Sub extends MyTest {
				}
				""", false, null).findPrimaryType();
		JUnitStatusRegistry registry= new JUnitStatusRegistry();
		// the runner reports the concrete class
		registry.record(1, fProject.getElementName(), "p.Sub", "a", null, Result.FAILURE);
		assertThat(findResult(registry, sub)).isEqualTo(Result.FAILURE);
		assertThat(findResult(registry, base)).isNull();
		assertThat(findResult(registry, base.getMethod("a", new String[0]))).isNull();
	}

	@Test
	public void testNewRunReplacesResultsWhateverTheParameterTypes() throws Exception {
		IType type= createType(JUnitCore.JUNIT5_CONTAINER_PATH, TestKindRegistry.JUNIT5_TEST_KIND_ID, """
				package p;
				import org.junit.jupiter.api.Test;
				public class MyTest {
					@Test void a() {}
				}
				""");
		IMethod a= type.getMethod("a", new String[0]);
		String project= fProject.getElementName();
		JUnitStatusRegistry registry= new JUnitStatusRegistry();
		registry.record(1, project, "p.MyTest", "a", null, Result.FAILURE);
		registry.record(1, project, "p.MyTest", "a", new String[0], Result.OK);
		assertThat(findResult(registry, a)).isEqualTo(Result.FAILURE); // same run: merged
		registry.record(2, project, "p.MyTest", "a", new String[0], Result.OK);
		assertThat(findResult(registry, a)).isEqualTo(Result.OK); // the old FAILURE without types is gone
	}

	@Test
	public void testTestCaseSubclassesInJUnit4Project() throws Exception {
		IType type= createType(JUnitCore.JUNIT4_CONTAINER_PATH, TestKindRegistry.JUNIT4_TEST_KIND_ID, """
				package p;
				public class MyTest extends junit.framework.TestCase {
					public void testRun() {}
				}
				""");
		assertThat(isTest(type, "testRun", TestKindRegistry.JUNIT4_TEST_KIND_ID)).isTrue();
		IType abstractType= type.getPackageFragment().createCompilationUnit("Abstract.java", """
				package p;
				public abstract class Abstract extends junit.framework.TestCase {
					public void testRun() {}
				}
				""", false, null).findPrimaryType();
		assertThat(isTest(abstractType, "testRun", TestKindRegistry.JUNIT4_TEST_KIND_ID)).isFalse();
	}

	@Test
	public void testNewRunKeepsTheMethodsItDidNotRun() throws Exception {
		IType type= createType(JUnitCore.JUNIT4_CONTAINER_PATH, TestKindRegistry.JUNIT4_TEST_KIND_ID, """
				package p;
				import org.junit.Test;
				public class MyTest {
					@Test public void a() {}
					@Test public void b() {}
				}
				""");
		String project= fProject.getElementName();
		JUnitStatusRegistry registry= new JUnitStatusRegistry();
		registry.record(1, project, "p.MyTest", "a", null, Result.FAILURE);
		registry.record(1, project, "p.MyTest", "b", null, Result.OK);
		// the second run only runs b
		registry.record(2, project, "p.MyTest", "b", null, Result.OK);
		assertThat(findResult(registry, type.getMethod("a", new String[0]))).isEqualTo(Result.FAILURE);
		assertThat(findResult(registry, type)).isEqualTo(Result.FAILURE);
	}

	@Test
	public void testRenamedMethodOfSubclassIsIgnored() throws Exception {
		IType type= createType(JUnitCore.JUNIT3_CONTAINER_PATH, TestKindRegistry.JUNIT3_TEST_KIND_ID, """
				package p;
				public class MyTest extends junit.framework.TestCase {
					public void testNewName() {}
				}
				""");
		JUnitStatusRegistry registry= new JUnitStatusRegistry();
		registry.record(1, fProject.getElementName(), "p.MyTest", "testOldName", null, Result.FAILURE);
		assertThat(findResult(registry, type)).isNull();
		registry.record(1, fProject.getElementName(), "p.MyTest", "testNewName", null, Result.OK);
		assertThat(findResult(registry, type)).isEqualTo(Result.OK);
	}

	@Test
	public void testResultsOfRunsLaunchedFromAnotherProject() throws Exception {
		IType type= createType(JUnitCore.JUNIT4_CONTAINER_PATH, TestKindRegistry.JUNIT4_TEST_KIND_ID, """
				package p;
				import org.junit.Test;
				public class MyTest {
					@Test public void a() {}
				}
				""");
		IJavaProject launched= JavaProjectHelper.createJavaProject("LaunchedProject", "bin");
		try {
			JavaProjectHelper.addRTJar(launched);
			JavaProjectHelper.addRequiredProject(launched, fProject);
			JUnitStatusRegistry registry= new JUnitStatusRegistry();
			registry.record(1, "LaunchedProject", "p.MyTest", "a", null, Result.FAILURE);
			assertThat(findResult(registry, type)).isEqualTo(Result.FAILURE);
			registry.record(1, "UnrelatedProject", "p.MyTest", "a", null, Result.ERROR);
			assertThat(findResult(registry, type)).isEqualTo(Result.FAILURE);
		} finally {
			JavaProjectHelper.delete(launched);
		}
	}

	@Test
	public void testParameterTypesWrittenDifferentlyFromTheSource() throws Exception {
		IType type= createType(JUnitCore.JUNIT5_CONTAINER_PATH, TestKindRegistry.JUNIT5_TEST_KIND_ID, """
				package p;
				import org.junit.jupiter.params.ParameterizedTest;
				public class MyTest {
					@ParameterizedTest <T extends Number> void a(T t, p.MyTest.Inner i) {}
					static class Inner {}
				}
				""");
		IMethod a= type.getMethods()[0];
		JUnitStatusRegistry registry= new JUnitStatusRegistry();
		registry.record(1, fProject.getElementName(), "p.MyTest", "a", new String[] { "java.lang.Number", "p.MyTest$Inner" }, Result.FAILURE);
		assertThat(findResult(registry, a)).isEqualTo(Result.FAILURE);
	}

	@Test
	public void testLatestRunWinsWhateverTheLaunchedProject() throws Exception {
		IType type= createType(JUnitCore.JUNIT4_CONTAINER_PATH, TestKindRegistry.JUNIT4_TEST_KIND_ID, """
				package p;
				import org.junit.Test;
				public class MyTest {
					@Test public void a() {}
				}
				""");
		IJavaProject launched= JavaProjectHelper.createJavaProject("LaunchedProject", "bin");
		try {
			JavaProjectHelper.addRTJar(launched);
			JavaProjectHelper.addRequiredProject(launched, fProject);
			JUnitStatusRegistry registry= new JUnitStatusRegistry();
			registry.record(1, fProject.getElementName(), "p.MyTest", "a", null, Result.FAILURE);
			registry.record(2, "LaunchedProject", "p.MyTest", "a", null, Result.OK);
			assertThat(findResult(registry, type)).isEqualTo(Result.OK);
			registry.record(3, fProject.getElementName(), "p.MyTest", "a", null, Result.FAILURE);
			assertThat(findResult(registry, type)).isEqualTo(Result.FAILURE);
		} finally {
			JavaProjectHelper.delete(launched);
		}
	}

	@Test
	public void testMemberClassNamesWithDollarOrDot() throws Exception {
		IType type= createType(JUnitCore.JUNIT4_CONTAINER_PATH, TestKindRegistry.JUNIT4_TEST_KIND_ID, """
				package p;
				import org.junit.Test;
				public class MyTest {
					public static class Inner {
						@Test public void a() {}
					}
				}
				""");
		IType inner= type.getType("Inner");
		String project= fProject.getElementName();
		JUnitStatusRegistry registry= new JUnitStatusRegistry();
		registry.record(1, project, "p.MyTest$Inner", "a", null, Result.OK); // JUnit 5
		assertThat(findResult(registry, inner)).isEqualTo(Result.OK);
		registry.record(2, project, "p.MyTest.Inner", "a", null, Result.FAILURE); // JUnit 3 and 4
		assertThat(findResult(registry, inner)).isEqualTo(Result.FAILURE);
	}

	@Test
	public void testEnclosedMemberClasses() throws Exception {
		IType outer= createType(JUnitCore.JUNIT4_CONTAINER_PATH, TestKindRegistry.JUNIT4_TEST_KIND_ID, """
				package p;
				import org.junit.Test;
				import org.junit.runner.RunWith;
				import org.junit.experimental.runners.Enclosed;
				@RunWith(Enclosed.class)
				public class MyTest {
					public static class Inner {
						@Test public void a() {}
					}
					public static class Other {
						@Test public void b() {}
					}
				}
				""");
		IType inner= outer.getType("Inner");
		String project= fProject.getElementName();
		JUnitStatusRegistry registry= new JUnitStatusRegistry();
		assertThat(findResult(registry, outer)).isNull();
		// the JUnit 4 runner names the member classes with a dot in the test names
		registry.record(1, project, "p.MyTest.Inner", "a", null, Result.OK);
		registry.record(1, project, "p.MyTest.Other", "b", null, Result.FAILURE);
		assertThat(findResult(registry, inner.getMethod("a", new String[0]))).isEqualTo(Result.OK);
		assertThat(findResult(registry, inner)).isEqualTo(Result.OK);
		assertThat(findResult(registry, outer.getType("Other"))).isEqualTo(Result.FAILURE);
		assertThat(findResult(registry, outer)).isEqualTo(Result.FAILURE);
		assertThat(TestKindRegistry.getContainerTestKind(inner).getFinder().isTest(inner)).isTrue();
	}

	@Test
	public void testTestNGTestsAreNotJUnitTests() throws Exception {
		IType type= createType(JUnitCore.JUNIT4_CONTAINER_PATH, TestKindRegistry.JUNIT4_TEST_KIND_ID, """
				package p;
				import org.testng.annotations.Test;
				public class MyTest {
					@Test public void a() {}
					public void testB() {}
				}
				""");
		IPackageFragmentRoot root= (IPackageFragmentRoot) type.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
		root.createPackageFragment("org.testng.annotations", true, null).createCompilationUnit("Test.java", """
				package org.testng.annotations;
				public @interface Test {}
				""", false, null);
		String kind= TestKindRegistry.JUNIT4_TEST_KIND_ID;
		assertThat(isTest(type, "a", kind)).isFalse();
		assertThat(isTest(type, "testB", kind)).isFalse();
		assertThat(TestKindRegistry.getContainerTestKind(type).getFinder().isTest(type)).isFalse();
	}

	@Test
	public void testEncodedParameterTypesOfOverloads() throws Exception {
		IType type= createType(JUnitCore.JUNIT5_CONTAINER_PATH, TestKindRegistry.JUNIT5_TEST_KIND_ID, """
				package p;
				import org.junit.jupiter.params.ParameterizedTest;
				public class MyTest {
					@ParameterizedTest void a(String[] s) {}
					@ParameterizedTest void a(int i) {}
				}
				""");
		var reference= JUnitStatusRegistry.parseUniqueId("[engine:junit-jupiter]/[class:p.MyTest]/[test-template:a(java.lang.String%5B%5D)]/[test-template-invocation:#1]");
		assertThat(reference.parameterTypes()).containsExactly("java.lang.String[]");
		assertThat(JUnitStatusRegistry.parseUniqueId("[class:p.A]/[method:a%2B(int)]").methodName()).isEqualTo("a+");
		// a supplementary character, raw and encoded
		assertThat(JUnitStatusRegistry.parseUniqueId("[class:p.A]/[method:x\uD83D\uDE00%F0%9F%98%80()]").methodName()).isEqualTo("x\uD83D\uDE00\uD83D\uDE00");
		IMethod array= type.getMethod("a", new String[] { "[QString;" });
		IMethod integer= type.getMethod("a", new String[] { "I" });
		JUnitStatusRegistry registry= new JUnitStatusRegistry();
		registry.record(1, fProject.getElementName(), reference.className(), reference.methodName(), reference.parameterTypes(), Result.FAILURE);
		assertThat(findResult(registry, array)).isEqualTo(Result.FAILURE);
		assertThat(findResult(registry, integer)).isNull();
	}

	@Test
	public void testEditedParametersDoNotGetTheOldResult() throws Exception {
		IType type= createType(JUnitCore.JUNIT5_CONTAINER_PATH, TestKindRegistry.JUNIT5_TEST_KIND_ID, """
				package p;
				import org.junit.jupiter.params.ParameterizedTest;
				public class MyTest {
					@ParameterizedTest void m(int i) {}
				}
				""");
		JUnitStatusRegistry registry= new JUnitStatusRegistry();
		// the result is the one of m(String), which has been edited to m(int)
		registry.record(1, fProject.getElementName(), "p.MyTest", "m", new String[] { "java.lang.String" }, Result.FAILURE);
		assertThat(findResult(registry, type.getMethod("m", new String[] { "I" }))).isNull();
	}

	@Test
	public void testUnresolvedOrOnDemandTestAnnotation() throws Exception {
		IType type= createType(JUnitCore.JUNIT4_CONTAINER_PATH, TestKindRegistry.JUNIT4_TEST_KIND_ID, """
				package p;
				import org.junit.*;
				public class MyTest {
					@Test public void onDemand() {}
					@Qualified public void other() {}
				}
				""");
		String kind= TestKindRegistry.JUNIT4_TEST_KIND_ID;
		assertThat(isTest(type, "onDemand", kind)).isTrue();
		// no import: the annotation is not resolved, JUnit is available
		IType unresolved= type.getPackageFragment().createCompilationUnit("Other.java", """
				package p;
				public class Other {
					@Test public void a() {}
					@org.testng.annotations.Test public void b() {}
				}
				""", false, null).findPrimaryType();
		assertThat(isTest(unresolved, "a", kind)).isTrue();
		assertThat(isTest(unresolved, "b", kind)).isFalse();
	}

	@Test
	public void testTestsInheritedFromInterfaceDefaultMethods() throws Exception {
		IType type= createType(JUnitCore.JUNIT5_CONTAINER_PATH, TestKindRegistry.JUNIT5_TEST_KIND_ID, """
				package p;
				public class MyTest implements MyTests {
				}
				""");
		type.getPackageFragment().createCompilationUnit("MyTests.java", """
				package p;
				import org.junit.jupiter.api.Test;
				public interface MyTests {
					@Test default void a() {}
				}
				""", false, null);
		JUnitStatusRegistry registry= new JUnitStatusRegistry();
		registry.record(1, fProject.getElementName(), "p.MyTest", "a", new String[0], Result.FAILURE);
		assertThat(findResult(registry, type)).isEqualTo(Result.FAILURE);
	}

	private static final String JUPITER_CLASS= "[engine:junit-jupiter]/[class:p.MyTest]"; //$NON-NLS-1$

	@Test
	public void testRerunReplacesTheResult() throws Exception {
		IType type= createType(JUnitCore.JUNIT5_CONTAINER_PATH, TestKindRegistry.JUNIT5_TEST_KIND_ID, """
				package p;
				import org.junit.jupiter.api.Test;
				public class MyTest {
					@Test void m() {}
				}
				""");
		TestRunSession session= new TestRunSession("rerun", fProject);
		TestCaseElement test= (TestCaseElement) session.createTestElement(session.getTestRoot(), "1", "m(p.MyTest)", false, 0, false, null, null, JUPITER_CLASS + "/[method:m()]");
		JUnitStatusRegistry registry= new JUnitStatusRegistry();
		session.registerTestFailureStatus(test, Status.FAILURE, "trace", null, null);
		assertThat(registry.recordRerun(test)).isTrue();
		assertThat(findResult(registry, type.getMethods()[0])).isEqualTo(Result.FAILURE);
		// the same test, run again from the JUnit view and passing
		session.registerTestFailureStatus(test, Status.OK, null, null, null);
		assertThat(registry.recordRerun(test)).isTrue();
		assertThat(findResult(registry, type.getMethods()[0])).isEqualTo(Result.OK);
		assertThat(findResult(registry, type)).isEqualTo(Result.OK);
	}

	@Test
	public void testRerunDoesNotReplaceTheResultOfALaterRun() throws Exception {
		IType type= createType(JUnitCore.JUNIT5_CONTAINER_PATH, TestKindRegistry.JUNIT5_TEST_KIND_ID, """
				package p;
				import org.junit.jupiter.api.Test;
				public class MyTest {
					@Test void m() {}
				}
				""");
		TestRunSession session= new TestRunSession("rerun", fProject);
		TestCaseElement test= (TestCaseElement) session.createTestElement(session.getTestRoot(), "1", "m(p.MyTest)", false, 0, false, null, null, JUPITER_CLASS + "/[method:m()]");
		JUnitStatusRegistry registry= new JUnitStatusRegistry();
		session.registerTestFailureStatus(test, Status.FAILURE, "trace", null, null);
		registry.recordRerun(test); // the session is the first run seen by the registry
		// a later run, which is not the session of the test
		registry.record(2, fProject.getElementName(), "p.MyTest", "m", new String[0], Result.OK);
		assertThat(findResult(registry, type.getMethods()[0])).isEqualTo(Result.OK);
		// the rerun of the first session does not hide it
		registry.recordRerun(test);
		assertThat(findResult(registry, type.getMethods()[0])).isEqualTo(Result.OK);
	}

	@Test
	public void testOutcomeOfTestContainers() throws Exception {
		IType type= createType(JUnitCore.JUNIT5_CONTAINER_PATH, TestKindRegistry.JUNIT5_TEST_KIND_ID, """
				package p;
				import org.junit.jupiter.api.Test;
				import org.junit.jupiter.params.ParameterizedTest;
				public class MyTest {
					@Test void a() {}
					@ParameterizedTest void disabled(String s) {}
				}
				""");
		IMethod a= type.getMethod("a", new String[0]);
		IMethod disabled= type.getMethod("disabled", new String[] { "QString;" });
		JUnitStatusRegistry registry= new JUnitStatusRegistry();
		TestRunSession failed= new TestRunSession("failed", fProject);
		TestSuiteElement container= (TestSuiteElement) failed.createTestElement(failed.getTestRoot(), "1", "p.MyTest", true, 0, false, null, null, JUPITER_CLASS);
		TestCaseElement test= (TestCaseElement) failed.createTestElement(container, "2", "a(p.MyTest)", false, 0, false, null, null, JUPITER_CLASS + "/[method:a()]");
		TestSuiteElement template= (TestSuiteElement) failed.createTestElement(container, "3", "disabled(java.lang.String)", true, 0, false, null, null,
				JUPITER_CLASS + "/[test-template:disabled(java.lang.String)]");
		template.setIgnored(true);
		failed.registerTestEnded(template, true);
		failed.registerTestEnded(test, true);
		// the test passes, but the @AfterAll method of the class fails
		failed.registerTestFailureStatus(container, Status.FAILURE, "afterAll", null, null);
		registry.recordRerun(test);
		registry.recordContainers(failed);
		assertThat(findResult(registry, a)).isEqualTo(Result.OK);
		assertThat(findResult(registry, disabled)).isEqualTo(Result.IGNORED);
		assertThat(findResult(registry, type)).isEqualTo(Result.FAILURE);

		// a later run where everything passes replaces the failure of the class
		TestRunSession passed= new TestRunSession("passed", fProject);
		TestSuiteElement passedContainer= (TestSuiteElement) passed.createTestElement(passed.getTestRoot(), "1", "p.MyTest", true, 0, false, null, null, JUPITER_CLASS);
		TestCaseElement passedTest= (TestCaseElement) passed.createTestElement(passedContainer, "2", "a(p.MyTest)", false, 0, false, null, null, JUPITER_CLASS + "/[method:a()]");
		passed.registerTestEnded(passedTest, true);
		registry.recordRerun(passedTest);
		registry.recordContainers(passed);
		assertThat(findResult(registry, type)).isEqualTo(Result.OK);
	}

	@Test
	public void testNewRunKeepsTheOverloadsItDidNotRun() throws Exception {
		IType type= createType(JUnitCore.JUNIT5_CONTAINER_PATH, TestKindRegistry.JUNIT5_TEST_KIND_ID, """
				package p;
				import org.junit.jupiter.params.ParameterizedTest;
				public class MyTest {
					@ParameterizedTest void m(String s) {}
					@ParameterizedTest void m(int i) {}
				}
				""");
		IMethod string= type.getMethod("m", new String[] { "QString;" });
		IMethod integer= type.getMethod("m", new String[] { "I" });
		JUnitStatusRegistry registry= new JUnitStatusRegistry();
		String project= fProject.getElementName();
		registry.record(1, project, "p.MyTest", "m", new String[] { "java.lang.String" }, Result.FAILURE);
		registry.record(1, project, "p.MyTest", "m", new String[] { "int" }, Result.OK);
		registry.record(2, project, "p.MyTest", "m", new String[] { "int" }, Result.OK);
		assertThat(findResult(registry, string)).isEqualTo(Result.FAILURE);
		assertThat(findResult(registry, integer)).isEqualTo(Result.OK);
		assertThat(findResult(registry, type)).isEqualTo(Result.FAILURE);
		// the same, when the second run was launched from another project
		JUnitStatusRegistry other= new JUnitStatusRegistry();
		other.record(1, project, "p.MyTest", "m", new String[] { "java.lang.String" }, Result.FAILURE);
		other.record(2, "LaunchedProject", "p.MyTest", "m", new String[] { "int" }, Result.OK);
		IJavaProject launched= JavaProjectHelper.createJavaProject("LaunchedProject", "bin");
		try {
			JavaProjectHelper.addRTJar(launched);
			JavaProjectHelper.addRequiredProject(launched, fProject);
			assertThat(findResult(other, string)).isEqualTo(Result.FAILURE);
			assertThat(findResult(other, integer)).isEqualTo(Result.OK);
		} finally {
			JavaProjectHelper.delete(launched);
		}
	}

	@Test
	public void testInheritedMethodWithTheNameOfAnotherSignature() throws Exception {
		IType sub= createType(JUnitCore.JUNIT5_CONTAINER_PATH, TestKindRegistry.JUNIT5_TEST_KIND_ID, """
				package p;
				import org.junit.jupiter.params.ParameterizedTest;
				public class MyTest extends Base {
					@ParameterizedTest void m(int i) {}
				}
				""");
		sub.getPackageFragment().createCompilationUnit("Base.java", """
				package p;
				import org.junit.jupiter.api.Test;
				public class Base {
					@Test void m() {}
				}
				""", false, null);
		JUnitStatusRegistry registry= new JUnitStatusRegistry();
		String project= fProject.getElementName();
		registry.record(1, project, "p.MyTest", "m", new String[0], Result.FAILURE);
		registry.record(1, project, "p.MyTest", "m", new String[] { "int" }, Result.OK);
		assertThat(findResult(registry, sub)).isEqualTo(Result.FAILURE);
		// an overridden method is not counted again
		registry= new JUnitStatusRegistry();
		registry.record(1, project, "p.MyTest", "m", new String[] { "int" }, Result.OK);
		assertThat(findResult(registry, sub)).isEqualTo(Result.OK);
	}

	@Test
	public void testEditedArrayRankAndTypeOfParametersDoNotGetTheOldResult() throws Exception {
		IType type= createType(JUnitCore.JUNIT5_CONTAINER_PATH, TestKindRegistry.JUNIT5_TEST_KIND_ID, """
				package p;
				import org.junit.jupiter.params.ParameterizedTest;
				public class MyTest {
					@ParameterizedTest void array(String s) {}
					@ParameterizedTest void date(java.sql.Date d) {}
					@ParameterizedTest <T extends Number> void bound(T t) {}
				}
				""");
		JUnitStatusRegistry registry= new JUnitStatusRegistry();
		String project= fProject.getElementName();
		// the results are the ones of array(String[]), date(java.util.Date), and of a bound of another type
		registry.record(1, project, "p.MyTest", "array", new String[] { "java.lang.String[]" }, Result.FAILURE);
		registry.record(1, project, "p.MyTest", "date", new String[] { "java.util.Date" }, Result.FAILURE);
		registry.record(1, project, "p.MyTest", "bound", new String[] { "java.lang.Object" }, Result.FAILURE);
		assertThat(findResult(registry, type.getMethod("array", new String[] { "QString;" }))).isNull();
		assertThat(findResult(registry, type.getMethod("date", new String[] { "Qjava.sql.Date;" }))).isNull();
		assertThat(findResult(registry, type.getMethod("bound", new String[] { "QT;" }))).isNull();
		registry.record(2, project, "p.MyTest", "bound", new String[] { "java.lang.Number" }, Result.FAILURE);
		assertThat(findResult(registry, type.getMethod("bound", new String[] { "QT;" }))).isEqualTo(Result.FAILURE);
	}

	@Test
	public void testIndependentMemberClassIsNotPartOfTheOuterClass() throws Exception {
		IType outer= createType(JUnitCore.JUNIT4_CONTAINER_PATH, TestKindRegistry.JUNIT4_TEST_KIND_ID, """
				package p;
				import org.junit.Test;
				public class MyTest {
					@Test public void a() {}
					public static class Inner {
						@Test public void b() {}
					}
				}
				""");
		JUnitStatusRegistry registry= new JUnitStatusRegistry();
		String project= fProject.getElementName();
		registry.record(1, project, "p.MyTest", "a", null, Result.OK);
		registry.record(1, project, "p.MyTest.Inner", "b", null, Result.FAILURE);
		// an inner class which is not run by the runner of the outer class has its own status
		assertThat(findResult(registry, outer)).isEqualTo(Result.OK);
		assertThat(findResult(registry, outer.getType("Inner"))).isEqualTo(Result.FAILURE);
	}

	@Test
	public void testNestedMemberClassIsPartOfTheOuterClass() throws Exception {
		IType outer= createType(JUnitCore.JUNIT5_CONTAINER_PATH, TestKindRegistry.JUNIT5_TEST_KIND_ID, """
				package p;
				import org.junit.jupiter.api.Nested;
				import org.junit.jupiter.api.Test;
				public class MyTest {
					@Test void a() {}
					@Nested class Inner {
						@Test void b() {}
					}
				}
				""");
		JUnitStatusRegistry registry= new JUnitStatusRegistry();
		String project= fProject.getElementName();
		registry.record(1, project, "p.MyTest", "a", new String[0], Result.OK);
		registry.record(1, project, "p.MyTest$Inner", "b", new String[0], Result.FAILURE);
		assertThat(findResult(registry, outer)).isEqualTo(Result.FAILURE);
	}

	@Test
	public void testOverloadedGenericMethodMatchesTheErasedParameters() throws Exception {
		IType type= createType(JUnitCore.JUNIT5_CONTAINER_PATH, TestKindRegistry.JUNIT5_TEST_KIND_ID, """
				package p;
				import org.junit.jupiter.params.ParameterizedTest;
				public class MyTest {
					@ParameterizedTest <T extends Number> void m(T value) {}
					@ParameterizedTest void m(int value) {}
				}
				""");
		JUnitStatusRegistry registry= new JUnitStatusRegistry();
		registry.record(1, fProject.getElementName(), "p.MyTest", "m", new String[] { "java.lang.Number" }, Result.FAILURE);
		assertThat(findResult(registry, type.getMethod("m", new String[] { "QT;" }))).isEqualTo(Result.FAILURE);
		assertThat(findResult(registry, type.getMethod("m", new String[] { "I" }))).isNull();
		assertThat(findResult(registry, type)).isEqualTo(Result.FAILURE);
	}

	@Test
	public void testNewerResultReplacesAnOlderOneWithUnknownParameterTypesFromAnotherProject() throws Exception {
		IType type= createType(JUnitCore.JUNIT5_CONTAINER_PATH, TestKindRegistry.JUNIT5_TEST_KIND_ID, """
				package p;
				import org.junit.jupiter.api.Test;
				public class MyTest {
					@Test void a() {}
				}
				""");
		IJavaProject launched= JavaProjectHelper.createJavaProject("LaunchedProject", "bin");
		try {
			JavaProjectHelper.addRTJar(launched);
			JavaProjectHelper.addRequiredProject(launched, fProject);
			JUnitStatusRegistry registry= new JUnitStatusRegistry();
			registry.record(1, fProject.getElementName(), "p.MyTest", "a", null, Result.FAILURE); // JUnit 4 style, unknown types
			registry.record(2, "LaunchedProject", "p.MyTest", "a", new String[0], Result.OK);
			assertThat(findResult(registry, type.getMethod("a", new String[0]))).isEqualTo(Result.OK);
			assertThat(findResult(registry, type)).isEqualTo(Result.OK);
		} finally {
			JavaProjectHelper.delete(launched);
		}
	}

	@Test
	public void testSupersededResultsDoNotDependOnTheOrder() throws Exception {
		IType type= createType(JUnitCore.JUNIT5_CONTAINER_PATH, TestKindRegistry.JUNIT5_TEST_KIND_ID, """
				package p;
				import org.junit.jupiter.params.ParameterizedTest;
				public class MyTest {
					@ParameterizedTest void m(String s) {}
					@ParameterizedTest void m(int i) {}
				}
				""");
		IJavaProject first= JavaProjectHelper.createJavaProject("LaunchedProject", "bin");
		IJavaProject second= JavaProjectHelper.createJavaProject("LaunchedProject2", "bin");
		try {
			for (IJavaProject project : new IJavaProject[] { first, second }) {
				JavaProjectHelper.addRTJar(project);
				JavaProjectHelper.addRequiredProject(project, fProject);
			}
			// One result per launched project: [int] in run 1, unknown types in run 2, [String] in run 3. The unknown types
			// of run 2 supersede [int], and are superseded by [String]: only the last one is left, whatever the order.
			for (int i= 0; i < 20; i++) {
				JUnitStatusRegistry registry= new JUnitStatusRegistry();
				registry.record(1, fProject.getElementName(), "p.MyTest", "m", new String[] { "int" }, Result.FAILURE);
				registry.record(2, "LaunchedProject", "p.MyTest", "m", null, Result.OK);
				registry.record(3, "LaunchedProject2", "p.MyTest", "m", new String[] { "java.lang.String" }, Result.OK);
				assertThat(findResult(registry, type)).isEqualTo(Result.OK);
			}
		} finally {
			JavaProjectHelper.delete(first);
			JavaProjectHelper.delete(second);
		}
	}

	@Test
	public void testCompletedContainerClearsAnEarlierFailureWhateverItsTests() throws Exception {
		IType type= createType(JUnitCore.JUNIT5_CONTAINER_PATH, TestKindRegistry.JUNIT5_TEST_KIND_ID, """
				package p;
				import org.junit.jupiter.api.Test;
				public class MyTest {
					@Test void a() {}
				}
				""");
		JUnitStatusRegistry registry= new JUnitStatusRegistry();
		TestRunSession first= new TestRunSession("first", fProject);
		TestSuiteElement error= (TestSuiteElement) first.createTestElement(first.getTestRoot(), "1", "p.MyTest", true, 0, false, null, null, JUPITER_CLASS);
		first.registerTestFailureStatus(error, Status.ERROR, "beforeAll", null, null);
		registry.recordContainers(first);
		assertThat(findResult(registry, type)).isEqualTo(Result.ERROR);

		// the lifecycle method now works, and a test fails
		TestRunSession second= new TestRunSession("second", fProject);
		TestSuiteElement container= (TestSuiteElement) second.createTestElement(second.getTestRoot(), "1", "p.MyTest", true, 0, false, null, null, JUPITER_CLASS);
		TestCaseElement test= (TestCaseElement) second.createTestElement(container, "2", "a(p.MyTest)", false, 0, false, null, null, JUPITER_CLASS + "/[method:a()]");
		second.registerTestFailureStatus(test, Status.FAILURE, "trace", null, null);
		registry.recordRerun(test);
		registry.recordContainers(second);
		assertThat(findResult(registry, type)).isEqualTo(Result.FAILURE);
	}

	@Test
	public void testLegacyClassContainerFailureIsRecorded() throws Exception {
		IType type= createType(JUnitCore.JUNIT4_CONTAINER_PATH, TestKindRegistry.JUNIT4_TEST_KIND_ID, """
				package p;
				import org.junit.Test;
				public class MyTest {
					@Test public void a() {}
				}
				""");
		JUnitStatusRegistry registry= new JUnitStatusRegistry();
		// JUnit 4 has no unique id: a failure of @AfterClass is reported on the suite named after the class
		TestRunSession session= new TestRunSession("legacy", fProject);
		TestSuiteElement container= (TestSuiteElement) session.createTestElement(session.getTestRoot(), "1", "p.MyTest", true, 0, false, null, null, "");
		session.registerTestFailureStatus(container, Status.FAILURE, "afterClass", null, null);
		registry.record(1, fProject.getElementName(), "p.MyTest", "a", null, Result.OK);
		registry.recordContainers(session);
		assertThat(findResult(registry, type)).isEqualTo(Result.FAILURE);
	}

	@Test
	public void testInitializationErrorOfJUnit4IsAClassOutcome() throws Exception {
		IType type= createType(JUnitCore.JUNIT4_CONTAINER_PATH, TestKindRegistry.JUNIT4_TEST_KIND_ID, """
				package p;
				import org.junit.Test;
				public class MyTest {
					@Test public void a() {}
				}
				""");
		JUnitStatusRegistry registry= new JUnitStatusRegistry();
		registry.record(1, fProject.getElementName(), "p.MyTest", "a", null, Result.OK);
		// JUnit 4 reports an invalid class, such as a non-static @BeforeClass, as a test named initializationError
		TestRunSession session= new TestRunSession("init", fProject);
		TestCaseElement error= (TestCaseElement) session.createTestElement(session.getTestRoot(), "1", "initializationError(p.MyTest)", false, 0, false, null, null, "");
		session.registerTestFailureStatus(error, Status.ERROR, "trace", null, null);
		registry.recordRerun(error);
		assertThat(findResult(registry, type)).isEqualTo(Result.ERROR);
	}

	@Test
	public void testCompletedContainerDoesNotHideIgnoredInvocations() throws Exception {
		IType type= createType(JUnitCore.JUNIT5_CONTAINER_PATH, TestKindRegistry.JUNIT5_TEST_KIND_ID, """
				package p;
				import org.junit.jupiter.params.ParameterizedTest;
				public class MyTest {
					@ParameterizedTest void m(String s) {}
				}
				""");
		JUnitStatusRegistry registry= new JUnitStatusRegistry();
		// every invocation is aborted by an assumption: ignored, in a template container which completed
		TestRunSession session= new TestRunSession("ignored", fProject);
		TestSuiteElement container= (TestSuiteElement) session.createTestElement(session.getTestRoot(), "1", "p.MyTest", true, 0, false, null, null, JUPITER_CLASS);
		TestSuiteElement template= (TestSuiteElement) session.createTestElement(container, "2", "m(java.lang.String)", true, 0, false, null, null,
				JUPITER_CLASS + "/[test-template:m(java.lang.String)]");
		TestCaseElement invocation= (TestCaseElement) session.createTestElement(template, "3", "m(p.MyTest)", false, 0, false, null, null,
				JUPITER_CLASS + "/[test-template:m(java.lang.String)]/[test-template-invocation:#1]");
		session.registerTestFailureStatus(invocation, Status.OK, null, null, null);
		registry.record(1, fProject.getElementName(), "p.MyTest", "m", new String[] { "java.lang.String" }, Result.IGNORED);
		registry.recordContainers(session);
		assertThat(findResult(registry, type.getMethod("m", new String[] { "QString;" }))).isEqualTo(Result.IGNORED);
		assertThat(findResult(registry, type)).isEqualTo(Result.IGNORED);
	}

	@Test
	public void testVintageRunnerContainerIsAClass() throws Exception {
		IType type= createType(JUnitCore.JUNIT4_CONTAINER_PATH, TestKindRegistry.JUNIT4_TEST_KIND_ID, """
				package p;
				import org.junit.Test;
				public class MyTest {
					@Test public void a() {}
				}
				""");
		JUnitStatusRegistry registry= new JUnitStatusRegistry();
		registry.record(1, fProject.getElementName(), "p.MyTest", "a", null, Result.OK);
		TestRunSession session= new TestRunSession("vintage", fProject);
		TestSuiteElement runner= (TestSuiteElement) session.createTestElement(session.getTestRoot(), "1", "p.MyTest", true, 0, false, null, null,
				"[engine:junit-vintage]/[runner:p.MyTest]");
		session.registerTestFailureStatus(runner, Status.FAILURE, "afterClass", null, null);
		registry.recordContainers(session);
		assertThat(findResult(registry, type)).isEqualTo(Result.FAILURE);
	}

	@Test
	public void testComposedNestedAnnotationMakesAMemberClassPartOfTheOuterClass() throws Exception {
		IType outer= createType(JUnitCore.JUNIT5_CONTAINER_PATH, TestKindRegistry.JUNIT5_TEST_KIND_ID, """
				package p;
				import org.junit.jupiter.api.Test;
				public class MyTest {
					@Test void a() {}
					@MyNested class Inner {
						@Test void b() {}
					}
				}
				""");
		outer.getPackageFragment().createCompilationUnit("MyNested.java", """
				package p;
				import java.lang.annotation.Retention;
				import java.lang.annotation.RetentionPolicy;
				@Retention(RetentionPolicy.RUNTIME)
				@org.junit.jupiter.api.Nested
				public @interface MyNested {
				}
				""", false, null);
		JUnitStatusRegistry registry= new JUnitStatusRegistry();
		String project= fProject.getElementName();
		registry.record(1, project, "p.MyTest", "a", new String[0], Result.OK);
		registry.record(1, project, "p.MyTest$Inner", "b", new String[0], Result.FAILURE);
		assertThat(findResult(registry, outer)).isEqualTo(Result.FAILURE);
	}

	@Test
	public void testComposedTestAnnotationNamedTest() throws Exception {
		IType type= createType(JUnitCore.JUNIT5_CONTAINER_PATH, TestKindRegistry.JUNIT5_TEST_KIND_ID, """
				package p;
				public class MyTest {
					@Test void a() {}
				}
				""");
		type.getPackageFragment().createCompilationUnit("Test.java", """
				package p;
				import java.lang.annotation.Retention;
				import java.lang.annotation.RetentionPolicy;
				@Retention(RetentionPolicy.RUNTIME)
				@org.junit.jupiter.api.Test
				public @interface Test {
				}
				""", false, null);
		// the annotation of the package, composed from the Jupiter one, is a test annotation
		assertThat(isTest(type, "a", TestKindRegistry.JUNIT5_TEST_KIND_ID)).isTrue();
	}

	@Test
	public void testChainedTypeVariableBoundsErase() throws Exception {
		IType type= createType(JUnitCore.JUNIT5_CONTAINER_PATH, TestKindRegistry.JUNIT5_TEST_KIND_ID, """
				package p;
				import org.junit.jupiter.params.ParameterizedTest;
				public class MyTest {
					@ParameterizedTest <N extends Number, T extends N> void m(T value) {}
					@ParameterizedTest void m(int value) {}
				}
				""");
		JUnitStatusRegistry registry= new JUnitStatusRegistry();
		registry.record(1, fProject.getElementName(), "p.MyTest", "m", new String[] { "java.lang.Number" }, Result.FAILURE);
		assertThat(findResult(registry, type.getMethod("m", new String[] { "QT;" }))).isEqualTo(Result.FAILURE);
		assertThat(findResult(registry, type.getMethod("m", new String[] { "I" }))).isNull();
	}

	@Test
	public void testUnrelatedNestedAnnotationDoesNotMakeAMemberClassPartOfTheOuterClass() throws Exception {
		IType outer= createType(JUnitCore.JUNIT4_CONTAINER_PATH, TestKindRegistry.JUNIT4_TEST_KIND_ID, """
				package p;
				import org.junit.Test;
				public class MyTest {
					@Test public void a() {}
					@Nested public static class Inner {
						@Test public void b() {}
					}
				}
				""");
		outer.getPackageFragment().createCompilationUnit("Nested.java", """
				package p;
				public @interface Nested {
				}
				""", false, null);
		JUnitStatusRegistry registry= new JUnitStatusRegistry();
		String project= fProject.getElementName();
		registry.record(1, project, "p.MyTest", "a", null, Result.OK);
		registry.record(1, project, "p.MyTest.Inner", "b", null, Result.FAILURE);
		assertThat(findResult(registry, outer)).isEqualTo(Result.OK);
	}

	@Test
	public void testRunnerWithEnclosedInItsNameIsNotEnclosed() throws Exception {
		IType outer= createType(JUnitCore.JUNIT4_CONTAINER_PATH, TestKindRegistry.JUNIT4_TEST_KIND_ID, """
				package p;
				import org.junit.Test;
				import org.junit.runner.RunWith;
				@RunWith(NotEnclosed.class)
				public class MyTest {
					@Test public void a() {}
					public static class Inner {
						@Test public void b() {}
					}
				}
				""");
		outer.getPackageFragment().createCompilationUnit("NotEnclosed.java", """
				package p;
				public class NotEnclosed extends org.junit.runners.BlockJUnit4ClassRunner {
					public NotEnclosed(Class<?> type) throws Exception {
						super(type);
					}
				}
				""", false, null);
		JUnitStatusRegistry registry= new JUnitStatusRegistry();
		String project= fProject.getElementName();
		registry.record(1, project, "p.MyTest", "a", null, Result.OK);
		registry.record(1, project, "p.MyTest.Inner", "b", null, Result.FAILURE);
		assertThat(findResult(registry, outer)).isEqualTo(Result.OK);
	}

	@Test
	public void testBinaryComposedAnnotationIsResolved() throws Exception {
		IType type= createType(JUnitCore.JUNIT5_CONTAINER_PATH, TestKindRegistry.JUNIT5_TEST_KIND_ID, """
				package p;
				import org.junit.jupiter.api.Test;
				public class MyTest {
					@Test void a() {}
				}
				""");
		// the Jupiter @Test is a binary type, meta-annotated with @Testable
		assertThat(TestSearchEngine.isOrIsMetaAnnotatedWith(type.getMethod("a", new String[0]).getAnnotations()[0], type, fProject,
				"org.junit.platform.commons.annotation.Testable")).isTrue();
	}

	@Test
	public void testResultsOfDeletedProjectLaunchedFromADependentProjectAreForgotten() throws Exception {
		String source= """
				package p;
				import org.junit.Test;
				public class MyTest {
					@Test public void a() {}
				}
				""";
		IType type= createType(JUnitCore.JUNIT4_CONTAINER_PATH, TestKindRegistry.JUNIT4_TEST_KIND_ID, source);
		IJavaProject launched= JavaProjectHelper.createJavaProject("LaunchedProject", "bin");
		JUnitStatusRegistry registry= new JUnitStatusRegistry();
		registry.start();
		try {
			JavaProjectHelper.addRTJar(launched);
			JavaProjectHelper.addRequiredProject(launched, fProject);
			registry.record(1, "LaunchedProject", "p.MyTest", "a", null, Result.FAILURE);
			assertThat(findResult(registry, type)).isEqualTo(Result.FAILURE);
			// not JavaProjectHelper.delete, which empties the classpath of the project before
			fProject.getProject().delete(IResource.FORCE | IResource.ALWAYS_DELETE_PROJECT_CONTENT, null);
			// the project is created again with the same classes, which have never run
			type= createType(JUnitCore.JUNIT4_CONTAINER_PATH, TestKindRegistry.JUNIT4_TEST_KIND_ID, source);
			assertThat(findResult(registry, type)).isNull();
		} finally {
			registry.dispose();
			JavaProjectHelper.delete(launched);
		}
	}

	@Test
	public void testLegacyContainerOfNestedClassIsRecorded() throws Exception {
		IType type= createType(JUnitCore.JUNIT4_CONTAINER_PATH, TestKindRegistry.JUNIT4_TEST_KIND_ID, """
				package p;
				import org.junit.Test;
				public class MyTest {
					public static class Inner {
						@Test public void a() {}
					}
				}
				""");
		IType inner= type.getType("Inner");
		JUnitStatusRegistry registry= new JUnitStatusRegistry();
		// the name of the suite of a nested class has a dollar, unlike the name of its class
		TestRunSession session= new TestRunSession("nested", fProject);
		TestSuiteElement container= (TestSuiteElement) session.createTestElement(session.getTestRoot(), "1", "p.MyTest$Inner", true, 0, false, null, null, "");
		session.registerTestFailureStatus(container, Status.FAILURE, "afterClass", null, null);
		registry.record(1, fProject.getElementName(), "p.MyTest$Inner", "a", null, Result.OK);
		registry.recordContainers(session);
		assertThat(findResult(registry, inner)).isEqualTo(Result.FAILURE);
	}

	@Test
	public void testResultsOfMethodsWhichAreNoLongerTestsAreIgnored() throws Exception {
		IType type= createType(JUnitCore.JUNIT4_CONTAINER_PATH, TestKindRegistry.JUNIT4_TEST_KIND_ID, """
				package p;
				import org.junit.Test;
				public class MyTest extends Base {
					@Test public void b() {}
					public void a() {}
				}
				""");
		type.getPackageFragment().createCompilationUnit("Base.java", """
				package p;
				import org.junit.Test;
				public class Base {
					@Test public void c() {}
					public void d() {}
				}
				""", false, null);
		String project= fProject.getElementName();
		JUnitStatusRegistry registry= new JUnitStatusRegistry();
		// a and d lost their annotation, but are still methods of the class
		registry.record(1, project, "p.MyTest", "a", null, Result.FAILURE);
		registry.record(1, project, "p.MyTest", "d", null, Result.FAILURE);
		registry.record(1, project, "p.MyTest", "b", null, Result.OK);
		registry.record(1, project, "p.MyTest", "c", null, Result.OK);
		assertThat(findResult(registry, type)).isEqualTo(Result.OK);
		registry.record(2, project, "p.MyTest", "c", null, Result.FAILURE);
		assertThat(findResult(registry, type)).isEqualTo(Result.FAILURE);
	}

	@Test
	public void testQualifiedAndComposedJupiterAnnotations() throws Exception {
		IType type= createType(JUnitCore.JUNIT5_CONTAINER_PATH, TestKindRegistry.JUNIT5_TEST_KIND_ID, """
				package p;
				public class MyTest {
					@org.junit.jupiter.params.ParameterizedTest void qualified() {}
					@org.junit.jupiter.api.RepeatedTest(2) void qualifiedRepeated() {}
					@FastTest void composed() {}
					@Fast void composedTwice() {}
				}
				""");
		type.getPackageFragment().createCompilationUnit("FastTest.java", """
				package p;
				import java.lang.annotation.Retention;
				import java.lang.annotation.RetentionPolicy;
				@Retention(RetentionPolicy.RUNTIME)
				@org.junit.jupiter.params.ParameterizedTest
				public @interface FastTest {
				}
				""", false, null);
		type.getPackageFragment().createCompilationUnit("Fast.java", """
				package p;
				import java.lang.annotation.Retention;
				import java.lang.annotation.RetentionPolicy;
				@Retention(RetentionPolicy.RUNTIME)
				@FastTest
				public @interface Fast {
				}
				""", false, null);
		String kind= TestKindRegistry.JUNIT5_TEST_KIND_ID;
		assertThat(isTest(type, "qualified", kind)).isTrue();
		assertThat(isTest(type, "qualifiedRepeated", kind)).isTrue();
		assertThat(isTest(type, "composed", kind)).isTrue();
		assertThat(isTest(type, "composedTwice", kind)).isTrue();
	}

	@Test
	public void testIgnoredJUnit4ClassIsAClassOutcome() throws Exception {
		IType type= createType(JUnitCore.JUNIT4_CONTAINER_PATH, TestKindRegistry.JUNIT4_TEST_KIND_ID, """
				package p;
				import org.junit.Ignore;
				import org.junit.Test;
				@Ignore
				public class MyTest {
					@Test public void a() {}
				}
				""");
		JUnitStatusRegistry registry= new JUnitStatusRegistry();
		// JUnit 4 reports an ignored class as a single test named after the class
		TestRunSession session= new TestRunSession("ignored", fProject);
		TestCaseElement test= (TestCaseElement) session.createTestElement(session.getTestRoot(), "1", "p.MyTest", false, 1, false, null, null, "");
		test.setIgnored(true);
		session.registerTestEnded(test, true);
		registry.recordRerun(test);
		assertThat(findResult(registry, type)).isEqualTo(Result.IGNORED);
	}

	@Test
	public void testAbortedContainerIsIgnored() throws Exception {
		IType type= createType(JUnitCore.JUNIT5_CONTAINER_PATH, TestKindRegistry.JUNIT5_TEST_KIND_ID, """
				package p;
				import org.junit.jupiter.api.Test;
				public class MyTest {
					@Test void a() {}
				}
				""");
		JUnitStatusRegistry registry= new JUnitStatusRegistry();
		TestRunSession first= new TestRunSession("passed", fProject);
		TestSuiteElement container= (TestSuiteElement) first.createTestElement(first.getTestRoot(), "1", "p.MyTest", true, 0, false, null, null, JUPITER_CLASS);
		TestCaseElement test= (TestCaseElement) first.createTestElement(container, "2", "a(p.MyTest)", false, 0, false, null, null, JUPITER_CLASS + "/[method:a()]");
		first.registerTestEnded(test, true);
		registry.recordRerun(test);
		assertThat(findResult(registry, type)).isEqualTo(Result.OK);

		// an assumption failed in @BeforeAll: the container is reported with an OK status and the assumption flag, its tests are not run
		TestRunSession session= new TestRunSession("aborted", fProject);
		container= (TestSuiteElement) session.createTestElement(session.getTestRoot(), "1", "p.MyTest", true, 0, false, null, null, JUPITER_CLASS);
		session.createTestElement(container, "2", "a(p.MyTest)", false, 0, false, null, null, JUPITER_CLASS + "/[method:a()]");
		container.setAssumptionFailed(true);
		session.registerTestFailureStatus(container, Status.OK, "assumption", null, null);
		registry.recordContainers(session);
		assertThat(findResult(registry, type)).isEqualTo(Result.IGNORED);
		assertThat(findResult(registry, type.getMethod("a", new String[0]))).isEqualTo(Result.IGNORED);
	}

	@Test
	public void testFactoryWithoutTestsReplacesAnEarlierFailure() throws Exception {
		IType type= createType(JUnitCore.JUNIT5_CONTAINER_PATH, TestKindRegistry.JUNIT5_TEST_KIND_ID, """
				package p;
				import java.util.List;
				import org.junit.jupiter.api.DynamicTest;
				import org.junit.jupiter.api.TestFactory;
				public class MyTest {
					@TestFactory List<DynamicTest> f() { return List.of(); }
				}
				""");
		IMethod factory= type.getMethod("f", new String[0]);
		String factoryId= JUPITER_CLASS + "/[test-factory:f()]";
		JUnitStatusRegistry registry= new JUnitStatusRegistry();
		TestRunSession first= new TestRunSession("first", fProject);
		TestSuiteElement container= (TestSuiteElement) first.createTestElement(first.getTestRoot(), "1", "p.MyTest", true, 0, false, null, null, JUPITER_CLASS);
		TestSuiteElement failing= (TestSuiteElement) first.createTestElement(container, "2", "f()", true, 0, false, null, null, factoryId);
		first.registerTestFailureStatus(failing, Status.FAILURE, "trace", null, null);
		registry.recordContainers(first);
		assertThat(findResult(registry, factory)).isEqualTo(Result.FAILURE);

		// the factory now returns no test: no event reports the end of its container
		TestRunSession second= new TestRunSession("second", fProject);
		container= (TestSuiteElement) second.createTestElement(second.getTestRoot(), "1", "p.MyTest", true, 0, false, null, null, JUPITER_CLASS);
		second.createTestElement(container, "2", "f()", true, 0, false, null, null, factoryId);
		registry.recordContainers(second);
		assertThat(findResult(registry, factory)).isEqualTo(Result.OK);
	}

	@Test
	public void testTestOverriddenByANonTestIsNotInherited() throws Exception {
		IType type= createType(JUnitCore.JUNIT5_CONTAINER_PATH, TestKindRegistry.JUNIT5_TEST_KIND_ID, """
				package p;
				import org.junit.jupiter.api.Test;
				public class MyTest extends Middle {
					@Test void b() {}
				}
				""");
		type.getPackageFragment().createCompilationUnit("Base.java", """
				package p;
				import org.junit.jupiter.api.Test;
				public class Base {
					@Test public void a() {}
				}
				""", false, null);
		type.getPackageFragment().createCompilationUnit("Middle.java", """
				package p;
				public class Middle extends Base {
					@Override public void a() {}
				}
				""", false, null);
		String project= fProject.getElementName();
		JUnitStatusRegistry registry= new JUnitStatusRegistry();
		registry.record(1, project, "p.MyTest", "a", null, Result.FAILURE);
		registry.record(1, project, "p.MyTest", "b", null, Result.OK);
		assertThat(findResult(registry, type)).isEqualTo(Result.OK);
	}

	@Test
	public void testAnnotationOfTheProjectNamedLikeAJupiterOneIsNotATest() throws Exception {
		IType type= createType(JUnitCore.JUNIT5_CONTAINER_PATH, TestKindRegistry.JUNIT5_TEST_KIND_ID, """
				package p;
				import org.junit.jupiter.api.Test;
				public class MyTest {
					@Test void a() {}
					@TestFactory void helper() {}
				}
				""");
		type.getPackageFragment().createCompilationUnit("TestFactory.java", """
				package p;
				public @interface TestFactory {
				}
				""", false, null);
		assertThat(isTest(type, "a", TestKindRegistry.JUNIT5_TEST_KIND_ID)).isTrue();
		assertThat(isTest(type, "helper", TestKindRegistry.JUNIT5_TEST_KIND_ID)).isFalse();
	}

	@Test
	public void testComposedParameterizedAnnotationNamedTest() throws Exception {
		IType type= createType(JUnitCore.JUNIT5_CONTAINER_PATH, TestKindRegistry.JUNIT5_TEST_KIND_ID, """
				package p;
				public class MyTest {
					@Test void a() {}
				}
				""");
		type.getPackageFragment().createCompilationUnit("Test.java", """
				package p;
				import java.lang.annotation.Retention;
				import java.lang.annotation.RetentionPolicy;
				@Retention(RetentionPolicy.RUNTIME)
				@org.junit.jupiter.params.ParameterizedTest
				public @interface Test {
				}
				""", false, null);
		assertThat(isTest(type, "a", TestKindRegistry.JUNIT5_TEST_KIND_ID)).isTrue();
	}

	@Test
	public void testComposedAnnotationDeclaredInTheFileOfTheTest() throws Exception {
		IType type= createType(JUnitCore.JUNIT5_CONTAINER_PATH, TestKindRegistry.JUNIT5_TEST_KIND_ID, """
				package p;
				import java.lang.annotation.Retention;
				import java.lang.annotation.RetentionPolicy;
				public class MyTest {
					@FastTest void a() {}
				}
				@Retention(RetentionPolicy.RUNTIME)
				@org.junit.jupiter.api.Test
				@interface FastTest {
				}
				""");
		assertThat(isTest(type, "a", TestKindRegistry.JUNIT5_TEST_KIND_ID)).isTrue();
	}

	@Test
	public void testJUnit4TestOverriddenByANonTestIsStillRun() throws Exception {
		IType type= createType(JUnitCore.JUNIT4_CONTAINER_PATH, TestKindRegistry.JUNIT4_TEST_KIND_ID, """
				package p;
				import org.junit.Test;
				public class MyTest extends Middle {
					@Test public void b() {}
				}
				""");
		type.getPackageFragment().createCompilationUnit("Base.java", """
				package p;
				import org.junit.Test;
				public class Base {
					@Test public void a() {}
				}
				""", false, null);
		type.getPackageFragment().createCompilationUnit("Middle.java", """
				package p;
				public class Middle extends Base {
					@Override public void a() {}
				}
				""", false, null);
		String project= fProject.getElementName();
		JUnitStatusRegistry registry= new JUnitStatusRegistry();
		// the runner of JUnit 4 runs the methods annotated in any superclass
		registry.record(1, project, "p.MyTest", "a", null, Result.FAILURE);
		registry.record(1, project, "p.MyTest", "b", null, Result.OK);
		assertThat(findResult(registry, type)).isEqualTo(Result.FAILURE);
	}

	@Test
	public void testDefaultMethodOverriddenByANonTestIsNotInherited() throws Exception {
		IType type= createType(JUnitCore.JUNIT5_CONTAINER_PATH, TestKindRegistry.JUNIT5_TEST_KIND_ID, """
				package p;
				import org.junit.jupiter.api.Test;
				public class MyTest implements B {
					@Test void b() {}
				}
				""");
		type.getPackageFragment().createCompilationUnit("A.java", """
				package p;
				import org.junit.jupiter.api.Test;
				public interface A {
					@Test default void t() {}
				}
				""", false, null);
		type.getPackageFragment().createCompilationUnit("B.java", """
				package p;
				public interface B extends A {
					@Override default void t() {}
				}
				""", false, null);
		String project= fProject.getElementName();
		JUnitStatusRegistry registry= new JUnitStatusRegistry();
		registry.record(1, project, "p.MyTest", "t", null, Result.FAILURE);
		registry.record(1, project, "p.MyTest", "b", null, Result.OK);
		assertThat(findResult(registry, type)).isEqualTo(Result.OK);
	}

	@Test
	public void testWorstResult() {
		assertThat(JUnitStatusRegistry.worst(null, null)).isNull();
		assertThat(JUnitStatusRegistry.worst(null, Result.IGNORED)).isEqualTo(Result.IGNORED);
		assertThat(JUnitStatusRegistry.worst(Result.IGNORED, Result.OK)).isEqualTo(Result.OK);
		assertThat(JUnitStatusRegistry.worst(Result.FAILURE, Result.OK)).isEqualTo(Result.FAILURE);
		assertThat(JUnitStatusRegistry.worst(Result.FAILURE, Result.ERROR)).isEqualTo(Result.ERROR);
	}

	@Test
	public void testPreferencesAreDisabledByDefault() {
		var store= PreferenceConstants.getPreferenceStore();
		assertThat(store.getDefaultBoolean(PreferenceConstants.EDITOR_JAVA_CODEMINING_SHOW_JUNIT_STATUS)).isFalse();
		assertThat(store.getDefaultBoolean(PreferenceConstants.EDITOR_JAVA_CODEMINING_SHOW_JUNIT_RUN)).isFalse();
		assertThat(store.getDefaultBoolean(PreferenceConstants.EDITOR_JAVA_CODEMINING_SHOW_JUNIT_DEBUG)).isFalse();
	}
}
