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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.stream.Collectors;

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
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.junit.model.TestCaseElement;
import org.eclipse.jdt.internal.junit.model.TestRunSession;
import org.eclipse.jdt.internal.junit.model.TestSuiteElement;
import org.eclipse.jdt.internal.junit.ui.EnumSourceValidator;
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

		assertEquals("ZETA", EnumSourceValidator.getEnumConstantForInvocation(method, 1)); //$NON-NLS-1$
		assertEquals("ALPHA", EnumSourceValidator.getEnumConstantForInvocation(method, 2)); //$NON-NLS-1$
		assertEquals("MIDDLE", EnumSourceValidator.getEnumConstantForInvocation(method, 3)); //$NON-NLS-1$
		assertCompiles(cu);
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

		assertEquals("INCLUDE", EnumSourceValidator.getEnumConstantForInvocation(method, 1)); //$NON-NLS-1$
		assertEquals("MATCH_NONE", EnumSourceValidator.getEnumConstantForInvocation(method, 5)); //$NON-NLS-1$
		assertNull(EnumSourceValidator.getEnumConstantForInvocation(method, 6));
		assertCompiles(cu);
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

		assertNotNull(first);
		assertNotNull(second);
		assertNotEquals(first, second);
		assertEquals("first.Color", Signature.toString(first.getParameterTypes()[0])); //$NON-NLS-1$
		assertEquals("second.Color", Signature.toString(second.getParameterTypes()[0])); //$NON-NLS-1$
		assertNull(TestMethodFinder.findMethod(type, "overloaded", new String[] { "Color" })); //$NON-NLS-1$ //$NON-NLS-2$
		assertCompiles(cu);
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

		assertNotNull(strings);
		assertNotNull(primitives);
		assertNotNull(nested);
		assertEquals("String[]", Signature.toString(strings.getParameterTypes()[0])); //$NON-NLS-1$
		assertEquals("int[][]", Signature.toString(primitives.getParameterTypes()[0])); //$NON-NLS-1$
		assertEquals("Value[]", Signature.toString(nested.getParameterTypes()[0])); //$NON-NLS-1$
		assertCompiles(cu);
	}

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

		assertFalse(EnumSourceValidator.canExcludeEnumValue(method, "RED")); //$NON-NLS-1$
		assertNull(EnumSourceValidator.getEnumConstantForInvocation(method, 1));
		assertCompiles(cu);
	}

	@Test
	public void testInvocationMappingRequiresUniqueId() throws Exception {
		ICompilationUnit cu= createCompilationUnit("test1", "MyTest.java", """
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
				"arbitrary display(test1.MyTest)", "not an enum value", false, null, //$NON-NLS-1$ //$NON-NLS-2$
				"[engine:junit-jupiter]/[class:test1.MyTest]/" //$NON-NLS-1$
						+ "[test-template:testWithEnum(test1.MyTest$Color)]/" //$NON-NLS-1$
						+ "[test-template-invocation:#2]"); //$NON-NLS-1$

		ExcludeParameterValueAction action= new ExcludeParameterValueAction();
		action.update(validCase);
		assertTrue(action.isEnabled());

		TestSuiteElement fallbackSuite= createParameterizedSuite(session, "fallback-suite", 3); //$NON-NLS-1$
		TestCaseElement withoutUniqueId= new TestCaseElement(fallbackSuite, "fallback-case-1", //$NON-NLS-1$
				"first(test1.MyTest)", "first", false, null, null); //$NON-NLS-1$ //$NON-NLS-2$
		new TestCaseElement(fallbackSuite, "fallback-case-2", //$NON-NLS-1$
				"second(test1.MyTest)", "second", false, null, null); //$NON-NLS-1$ //$NON-NLS-2$
		new TestCaseElement(fallbackSuite, "fallback-case-3", //$NON-NLS-1$
				"third(test1.MyTest)", "third", false, null, null); //$NON-NLS-1$ //$NON-NLS-2$

		action.update(withoutUniqueId);
		assertFalse(action.isEnabled());

		TestSuiteElement invalidSuite= createParameterizedSuite(session, "invalid-suite", 1); //$NON-NLS-1$
		TestCaseElement invalidCase= new TestCaseElement(invalidSuite, "invalid-case", //$NON-NLS-1$
				"invalid(test1.MyTest)", "invalid", false, null, //$NON-NLS-1$ //$NON-NLS-2$
				"[test-template-invocation:#99]"); //$NON-NLS-1$
		action.update(invalidCase);
		assertFalse(action.isEnabled());
		assertCompiles(cu);
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
		IPackageFragment pack= fSourceFolder.createPackageFragment(packageName, false, null);
		return pack.createCompilationUnit(unitName, source, false, null);
	}

	private static IMethod getMethod(ICompilationUnit cu, String name, String parameterSignature) {
		return cu.getType("MyTest").getMethod(name, new String[] { parameterSignature }); //$NON-NLS-1$
	}

	private static void assertCompiles(ICompilationUnit cu) {
		ASTParser parser= ASTParser.newParser(AST.getJLSLatest());
		parser.setSource(cu);
		parser.setResolveBindings(true);
		CompilationUnit astRoot= (CompilationUnit) parser.createAST(null);

		String errors= Arrays.stream(astRoot.getProblems())
				.filter(IProblem::isError)
				.map(IProblem::toString)
				.collect(Collectors.joining(System.lineSeparator()));
		assertEquals("", errors); //$NON-NLS-1$
	}
}
