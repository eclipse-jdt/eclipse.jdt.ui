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
