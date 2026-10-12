/*******************************************************************************
 * Copyright (c) 2026 Carsten Hammer and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Carsten Hammer - initial tests
 *******************************************************************************/
package org.eclipse.jdt.junit.tests;

import static org.eclipse.jdt.junit.tests.EnumSourceTestSupport.assertExclusionTarget;
import static org.eclipse.jdt.junit.tests.EnumSourceTestSupport.assertNoExclusionTarget;
import static org.eclipse.jdt.junit.tests.EnumSourceTestSupport.getMethod;
import static org.eclipse.jdt.junit.tests.EnumSourceTestSupport.sourceContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.junit.model.TestCaseElement;
import org.eclipse.jdt.internal.junit.model.TestRunSession;
import org.eclipse.jdt.internal.junit.model.TestSuiteElement;
import org.eclipse.jdt.internal.junit.ui.ExcludeParameterValueAction;
import org.eclipse.jdt.internal.junit.ui.TestMethodFinder;

import org.eclipse.jdt.ui.tests.core.rules.Java1d8ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

/**
 * Regression tests for conservative {@code @EnumSource} invocation mapping.
 */
public class EnumSourceSafetyTest {

	@Rule
	public ProjectTestSetup projectSetup= new Java1d8ProjectTestSetup();

	private IJavaProject fJProject;
	private IPackageFragmentRoot fSourceFolder;

	@Before
	public void setUp() throws Exception {
		fJProject= projectSetup.getProject();
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject, "src"); //$NON-NLS-1$

		JavaProjectHelper.addRTJar(fJProject);
		IClasspathEntry cpe= JavaCore.newContainerEntry(JUnitCore.JUNIT5_CONTAINER_PATH);
		JavaProjectHelper.addToClasspath(fJProject, cpe);
		JavaProjectHelper.set18CompilerOptions(fJProject);
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject, projectSetup.getDefaultClasspath());
	}

	@Test
	public void testEnumDeclarationOrderFromSeparateSourceFile() throws Exception {
		createCompilationUnit("test1.enums", "Color.java", """
				package test1.enums;

				public enum Color {
				    ZETA, ALPHA, MIDDLE
				}
				""");
		ICompilationUnit cu= createCompilationUnit("test1", "MyTest.java", """
				package test1;

				import org.junit.jupiter.params.ParameterizedTest;
				import org.junit.jupiter.params.provider.EnumSource;

				import test1.enums.Color;

				public class MyTest {
				    @ParameterizedTest
				    @EnumSource(Color.class)
				    public void testWithEnum(Color color) {
				    }
				}
				""");
		IMethod method= getMethod(cu, "testWithEnum", "QColor;"); //$NON-NLS-1$ //$NON-NLS-2$

		assertExclusionTarget(method, 1, "ZETA"); //$NON-NLS-1$
		assertExclusionTarget(method, 2, "ALPHA"); //$NON-NLS-1$
		assertExclusionTarget(method, 3, "MIDDLE"); //$NON-NLS-1$
	}

	@Test
	public void testEnumDeclarationOrderFromBinaryType() throws Exception {
		ICompilationUnit cu= createCompilationUnit("test1", "MyTest.java", """
				package test1;

				import org.junit.jupiter.params.ParameterizedTest;
				import org.junit.jupiter.params.provider.EnumSource;
				import org.junit.jupiter.params.provider.EnumSource.Mode;

				public class MyTest {
				    @ParameterizedTest
				    @EnumSource(Mode.class)
				    public void testWithEnum(Mode mode) {
				    }
				}
				""");
		IMethod method= getMethod(cu, "testWithEnum", "QMode;"); //$NON-NLS-1$ //$NON-NLS-2$

		assertExclusionTarget(method, 1, "INCLUDE"); //$NON-NLS-1$
		assertExclusionTarget(method, 5, "MATCH_NONE"); //$NON-NLS-1$
		assertNoExclusionTarget(method, 6);
	}

	@Test
	public void testMethodFinderDistinguishesEqualSimpleTypeNames() throws Exception {
		createCompilationUnit("first", "Color.java", """
				package first;
				public enum Color { RED }
				""");
		createCompilationUnit("second", "Color.java", """
				package second;
				public enum Color { BLUE }
				""");
		ICompilationUnit cu= createCompilationUnit("test1", "MyTest.java", """
				package test1;

				public class MyTest {
				    public void overloaded(first.Color value) {
				    }

				    public void overloaded(second.Color value) {
				    }
				}
				""");
		IType type= cu.getType("MyTest"); //$NON-NLS-1$

		IMethod first= TestMethodFinder.findMethod(
				type, "overloaded", new String[] { "first.Color" }); //$NON-NLS-1$ //$NON-NLS-2$
		IMethod second= TestMethodFinder.findMethod(
				type, "overloaded", new String[] { "second.Color" }); //$NON-NLS-1$ //$NON-NLS-2$

		assertNotNull("Expected the first.Color overload to be resolved in " + sourceContext(cu), first); //$NON-NLS-1$
		assertNotNull("Expected the second.Color overload to be resolved in " + sourceContext(cu), second); //$NON-NLS-1$
		assertNotEquals("Expected distinct methods for the two qualified parameter types in " + sourceContext(cu), first, second); //$NON-NLS-1$
		assertEquals("Expected parameter type for the resolved first.Color overload in " + sourceContext(cu), //$NON-NLS-1$
				"first.Color", Signature.toString(first.getParameterTypes()[0])); //$NON-NLS-1$
		assertEquals("Expected parameter type for the resolved second.Color overload in " + sourceContext(cu), //$NON-NLS-1$
				"second.Color", Signature.toString(second.getParameterTypes()[0])); //$NON-NLS-1$
		assertNull("Expected an unqualified parameter type to be rejected in " + sourceContext(cu), //$NON-NLS-1$
				TestMethodFinder.findMethod(type, "overloaded", new String[] { "Color" })); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testMethodFinderAcceptsReflectionArrayTypeNames() throws Exception {
		ICompilationUnit cu= createCompilationUnit("test1", "MyTest.java", """
				package test1;

				public class MyTest {
				    static class Value {
				    }

				    public void values(String[] value) {
				    }

				    public void values(int[][] value) {
				    }

				    public void values(Value[] value) {
				    }
				}
				""");
		IType type= cu.getType("MyTest"); //$NON-NLS-1$

		IMethod strings= TestMethodFinder.findMethod(
				type, "values", new String[] { "[Ljava.lang.String;" }); //$NON-NLS-1$ //$NON-NLS-2$
		IMethod primitives= TestMethodFinder.findMethod(
				type, "values", new String[] { "[[I" }); //$NON-NLS-1$ //$NON-NLS-2$
		IMethod nested= TestMethodFinder.findMethod(
				type, "values", new String[] { "[Ltest1.MyTest$Value;" }); //$NON-NLS-1$ //$NON-NLS-2$

		assertNotNull("Expected the String[] overload to be resolved in " + sourceContext(cu), strings); //$NON-NLS-1$
		assertNotNull("Expected the int[][] overload to be resolved in " + sourceContext(cu), primitives); //$NON-NLS-1$
		assertNotNull("Expected the nested Value[] overload to be resolved in " + sourceContext(cu), nested); //$NON-NLS-1$
		assertEquals("Expected parameter type for the resolved String[] overload in " + sourceContext(cu), //$NON-NLS-1$
				"String[]", Signature.toString(strings.getParameterTypes()[0])); //$NON-NLS-1$
		assertEquals("Expected parameter type for the resolved int[][] overload in " + sourceContext(cu), //$NON-NLS-1$
				"int[][]", Signature.toString(primitives.getParameterTypes()[0])); //$NON-NLS-1$
		assertEquals("Expected parameter type for the resolved Value[] overload in " + sourceContext(cu), //$NON-NLS-1$
				"Value[]", Signature.toString(nested.getParameterTypes()[0])); //$NON-NLS-1$
	}

	/**
	 * A ValueSource hidden behind five levels of meta-annotations is still an additional
	 * argument source. Together with EnumSource it makes invocation-index mapping
	 * ambiguous, so exclusion must be unavailable rather than guess an enum constant.
	 */
	@Test
	public void testDeepComposedArgumentSourceIsRejected() throws Exception {
		ICompilationUnit cu= createCompilationUnit("test1", "MyTest.java", """
				package test1;

				import java.lang.annotation.ElementType;
				import java.lang.annotation.Retention;
				import java.lang.annotation.RetentionPolicy;
				import java.lang.annotation.Target;

				import org.junit.jupiter.params.ParameterizedTest;
				import org.junit.jupiter.params.provider.EnumSource;
				import org.junit.jupiter.params.provider.ValueSource;

				public class MyTest {
				    enum Color { RED, GREEN }

				    @Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD })
				    @Retention(RetentionPolicy.RUNTIME)
				    @Level2
				    @interface Level1 {
				    }

				    @Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD })
				    @Retention(RetentionPolicy.RUNTIME)
				    @Level3
				    @interface Level2 {
				    }

				    @Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD })
				    @Retention(RetentionPolicy.RUNTIME)
				    @Level4
				    @interface Level3 {
				    }

				    @Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD })
				    @Retention(RetentionPolicy.RUNTIME)
				    @Level5
				    @interface Level4 {
				    }

				    @Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD })
				    @Retention(RetentionPolicy.RUNTIME)
				    @ValueSource(strings = "other")
				    @interface Level5 {
				    }

				    @ParameterizedTest
				    @EnumSource(Color.class)
				    @Level1
				    public void mixed(Object value) {
				    }
				}
				""");
		IMethod method= getMethod(cu, "mixed", "QObject;"); //$NON-NLS-1$ //$NON-NLS-2$

		assertNoExclusionTarget(method, 1);
	}

	@Test
	public void testActionEnabledForExplicitIncludeNames() throws Exception {
		ICompilationUnit cu= createCompilationUnit("test1", "MyTest.java", """
				package test1;

				import org.junit.jupiter.params.ParameterizedTest;
				import org.junit.jupiter.params.provider.EnumSource;

				public class MyTest {
				    enum Color { RED, GREEN, BLUE }

				    @ParameterizedTest
				    @EnumSource(value = Color.class, names = { "BLUE", "RED" })
				    public void testWithEnum(Color color) {
				    }
				}
				""");
		IMethod method= getMethod(cu, "testWithEnum", "QColor;"); //$NON-NLS-1$ //$NON-NLS-2$
		assertExclusionTarget(method, 2, "BLUE"); //$NON-NLS-1$

		TestRunSession session= new TestRunSession("EnumSource include run", fJProject); //$NON-NLS-1$
		TestSuiteElement suite= createParameterizedSuite(session, "include-suite", 2); //$NON-NLS-1$
		TestCaseElement blueCase= new TestCaseElement(suite, "include-blue", //$NON-NLS-1$
				"custom 2: BLUE(test1.MyTest)", "BLUE", true, null, //$NON-NLS-1$ //$NON-NLS-2$
				"[engine:junit-jupiter]/[class:test1.MyTest]/" //$NON-NLS-1$
						+ "[test-template:testWithEnum(test1.MyTest$Color)]/" //$NON-NLS-1$
						+ "[test-template-invocation:#2]"); //$NON-NLS-1$

		ExcludeParameterValueAction action= new ExcludeParameterValueAction();
		action.update(blueCase);
		assertTrue("Expected exclusion for the INCLUDE invocation " + blueCase.getUniqueId(), action.isEnabled()); //$NON-NLS-1$
	}

	@Test
	public void testInvocationMappingRequiresUniqueId() throws Exception {
		createCompilationUnit("test1", "MyTest.java", """
				package test1;

				import org.junit.jupiter.params.ParameterizedTest;
				import org.junit.jupiter.params.provider.EnumSource;

				public class MyTest {
				    enum Color { ZETA, ALPHA, MIDDLE }

				    @ParameterizedTest(name = "custom {index}: {0}")
				    @EnumSource(Color.class)
				    public void testWithEnum(Color color) {
				    }
				}
				""");

		TestRunSession session= new TestRunSession("EnumSource run", fJProject); //$NON-NLS-1$
		TestSuiteElement validSuite= createParameterizedSuite(session, "valid-suite", 1); //$NON-NLS-1$
		TestCaseElement validCase= new TestCaseElement(validSuite, "valid-case", //$NON-NLS-1$
				"arbitrary display(test1.MyTest)", "not an enum value", true, null, //$NON-NLS-1$ //$NON-NLS-2$
				"[engine:junit-jupiter]/[class:test1.MyTest]/" //$NON-NLS-1$
						+ "[test-template:testWithEnum(test1.MyTest$Color)]/" //$NON-NLS-1$
						+ "[test-template-invocation:#2]"); //$NON-NLS-1$

		ExcludeParameterValueAction action= new ExcludeParameterValueAction();
		action.update(validCase);
		assertTrue("Expected exclusion for the valid invocation " + validCase.getUniqueId(), action.isEnabled()); //$NON-NLS-1$

		TestSuiteElement fallbackSuite= createParameterizedSuite(session, "fallback-suite", 3); //$NON-NLS-1$
		TestCaseElement withoutUniqueId= new TestCaseElement(fallbackSuite, "fallback-case-1", //$NON-NLS-1$
				"first(test1.MyTest)", "first", true, null, null); //$NON-NLS-1$ //$NON-NLS-2$
		new TestCaseElement(fallbackSuite, "fallback-case-2", //$NON-NLS-1$
				"second(test1.MyTest)", "second", true, null, null); //$NON-NLS-1$ //$NON-NLS-2$
		new TestCaseElement(fallbackSuite, "fallback-case-3", //$NON-NLS-1$
				"third(test1.MyTest)", "third", true, null, null); //$NON-NLS-1$ //$NON-NLS-2$

		action.update(withoutUniqueId);
		assertFalse("Expected exclusion to be disabled without a unique ID", action.isEnabled()); //$NON-NLS-1$

		TestSuiteElement invalidSuite= createParameterizedSuite(session, "invalid-suite", 1); //$NON-NLS-1$
		TestCaseElement invalidCase= new TestCaseElement(invalidSuite, "invalid-case", //$NON-NLS-1$
				"invalid(test1.MyTest)", "invalid", true, null, //$NON-NLS-1$ //$NON-NLS-2$
				"[test-template-invocation:#99]"); //$NON-NLS-1$
		action.update(invalidCase);
		assertFalse("Expected exclusion to be disabled for " + invalidCase.getUniqueId(), action.isEnabled()); //$NON-NLS-1$
	}

	@Test
	public void testMethodFinderRejectsNonAdjacentOverloadsWithoutMetadata() throws Exception {
		ICompilationUnit cu= createCompilationUnit("test1", "MyTest.java", """
				package test1;
				public class MyTest {
				    public void overloaded(String value) {}
				    public void unrelated() {}
				    public void overloaded(int value) {}
				}
				""");
		IType type= cu.getType("MyTest"); //$NON-NLS-1$

		assertNull("Expected non-adjacent overloads to remain ambiguous without parameter metadata in " + sourceContext(cu), //$NON-NLS-1$
				TestMethodFinder.findMethod(type, "overloaded", null)); //$NON-NLS-1$
		assertEquals("Expected parameter metadata to select the String overload", //$NON-NLS-1$
				type.getMethod("overloaded", new String[] { "QString;" }), //$NON-NLS-1$ //$NON-NLS-2$
				TestMethodFinder.findMethod(type, "overloaded", new String[] { "java.lang.String" })); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testMethodFinderKeepsUniqueMatchAcrossUnrelatedMethods() throws Exception {
		ICompilationUnit cu= createCompilationUnit("test1", "MyTest.java", """
				package test1;
				public class MyTest {
				    public void unique(String value) {}
				    public void unrelated(int value) {}
				}
				""");
		IType type= cu.getType("MyTest"); //$NON-NLS-1$

		assertEquals("Expected an unrelated later method not to invalidate the unique match", //$NON-NLS-1$
				type.getMethod("unique", new String[] { "QString;" }), //$NON-NLS-1$ //$NON-NLS-2$
				TestMethodFinder.findMethod(type, "unique", null)); //$NON-NLS-1$
	}

	private TestSuiteElement createParameterizedSuite(TestRunSession session, String id, int childCount) {
		return new TestSuiteElement(session.getTestRoot(), id,
				"testWithEnum(test1.MyTest$Color)(test1.MyTest)", childCount, //$NON-NLS-1$
				"custom parameterized test", new String[] { "test1.MyTest$Color" }, //$NON-NLS-1$ //$NON-NLS-2$
				"[engine:junit-jupiter]/[class:test1.MyTest]/" //$NON-NLS-1$
						+ "[test-template:testWithEnum(test1.MyTest$Color)]"); //$NON-NLS-1$
	}

	private ICompilationUnit createCompilationUnit(String packageName, String unitName, String source)
			throws Exception {
		return EnumSourceTestSupport.createCompilationUnit(fSourceFolder, packageName, unitName, source, false);
	}
}
