/*******************************************************************************
 * Copyright (c) 2025 Carsten Hammer and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Carsten Hammer using github copilot - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.junit.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
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
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.junit.ui.EnumSourceValidator;
import org.eclipse.jdt.internal.junit.ui.TestMethodFinder;

import org.eclipse.jdt.ui.tests.core.rules.Java1d8ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

/**
 * Tests for safe {@code @EnumSource} exclusion and re-inclusion.
 */
public class EnumSourceFilterTest {

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
	public void testExcludePreservesRangeAndCompiles() throws Exception {
		ICompilationUnit cu= createCompilationUnit("""
				package test1;

				import org.junit.jupiter.params.ParameterizedTest;
				import org.junit.jupiter.params.provider.EnumSource;

				public class MyTest {
				    enum Color { RED, GREEN, BLUE }

				    @ParameterizedTest
				    @EnumSource(value = Color.class, from = "GREEN", to = "BLUE")
				    public void testWithEnum(Color color) {
				    }
				}
				""");
		IMethod method= getMethod(cu, "testWithEnum", "QColor;"); //$NON-NLS-1$ //$NON-NLS-2$

		assertEquals("GREEN", EnumSourceValidator.getEnumConstantForInvocation(method, 1)); //$NON-NLS-1$
		assertEquals("BLUE", EnumSourceValidator.getEnumConstantForInvocation(method, 2)); //$NON-NLS-1$
		assertNull(EnumSourceValidator.getEnumConstantForInvocation(method, 3));

		assertTrue(EnumSourceValidator.excludeEnumValue(method, "GREEN")); //$NON-NLS-1$

		String source= cu.getSource();
		assertTrue(source.contains("from = \"GREEN\"")); //$NON-NLS-1$
		assertTrue(source.contains("to = \"BLUE\"")); //$NON-NLS-1$
		assertTrue(source.contains("\"GREEN\"")); //$NON-NLS-1$
		assertEquals(List.of("GREEN"), EnumSourceValidator.getExcludedNames(method)); //$NON-NLS-1$
		assertCompiles(cu);
	}

	@Test
	public void testAppendExclusionWithoutDuplicates() throws Exception {
		ICompilationUnit cu= createCompilationUnit("""
				package test1;

				import org.junit.jupiter.params.ParameterizedTest;
				import org.junit.jupiter.params.provider.EnumSource;

				public class MyTest {
				    enum Color { RED, GREEN, BLUE }

				    @ParameterizedTest
				    @EnumSource(value = Color.class, mode = EnumSource.Mode.EXCLUDE, names = {"RED"})
				    public void testWithEnum(Color color) {
				    }
				}
				""");
		IMethod method= getMethod(cu, "testWithEnum", "QColor;"); //$NON-NLS-1$ //$NON-NLS-2$

		assertTrue(EnumSourceValidator.excludeEnumValue(method, "GREEN")); //$NON-NLS-1$
		assertFalse(EnumSourceValidator.excludeEnumValue(method, "GREEN")); //$NON-NLS-1$
		assertEquals(List.of("RED", "GREEN"), EnumSourceValidator.getExcludedNames(method)); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(1, countOccurrences(cu.getSource(), "\"GREEN\"")); //$NON-NLS-1$
		assertCompiles(cu);
	}

	@Test
	public void testIncludeAndRegexFiltersAreRejectedWithoutChanges() throws Exception {
		ICompilationUnit cu= createCompilationUnit("""
				package test1;

				import org.junit.jupiter.params.ParameterizedTest;
				import org.junit.jupiter.params.provider.EnumSource;

				public class MyTest {
				    enum Color { RED, GREEN, BLUE }

				    @ParameterizedTest
				    @EnumSource(names = {"RED", "GREEN"})
				    public void included(Color color) {
				    }

				    @ParameterizedTest
				    @EnumSource(mode = EnumSource.Mode.MATCH_ANY, names = "R.*")
				    public void matched(Color color) {
				    }
				}
				""");
		IMethod included= getMethod(cu, "included", "QColor;"); //$NON-NLS-1$ //$NON-NLS-2$
		IMethod matched= getMethod(cu, "matched", "QColor;"); //$NON-NLS-1$ //$NON-NLS-2$
		String original= cu.getSource();

		assertFalse(EnumSourceValidator.canExcludeEnumValue(included, "RED")); //$NON-NLS-1$
		assertFalse(EnumSourceValidator.canExcludeEnumValue(matched, "RED")); //$NON-NLS-1$
		assertFalse(EnumSourceValidator.excludeEnumValue(included, "RED")); //$NON-NLS-1$
		assertFalse(EnumSourceValidator.excludeEnumValue(matched, "RED")); //$NON-NLS-1$
		assertEquals(original, cu.getSource());
	}

	@Test
	public void testFullyQualifiedAnnotationRemainsCompilable() throws Exception {
		ICompilationUnit cu= createCompilationUnit("""
				package test1;

				import org.junit.jupiter.params.ParameterizedTest;

				public class MyTest {
				    enum Color { RED, GREEN }

				    @ParameterizedTest
				    @org.junit.jupiter.params.provider.EnumSource(Color.class)
				    public void testWithEnum(Color color) {
				    }
				}
				""");
		IMethod method= getMethod(cu, "testWithEnum", "QColor;"); //$NON-NLS-1$ //$NON-NLS-2$

		assertTrue(EnumSourceValidator.excludeEnumValue(method, "RED")); //$NON-NLS-1$

		assertTrue(cu.getSource().contains(
				"@org.junit.jupiter.params.provider.EnumSource(")); //$NON-NLS-1$
		assertCompiles(cu);
	}

	@Test
	public void testReincludeAllPreservesRangeAndSharedModeImport() throws Exception {
		ICompilationUnit cu= createCompilationUnit("""
				package test1;

				import org.junit.jupiter.params.ParameterizedTest;
				import org.junit.jupiter.params.provider.EnumSource;
				import org.junit.jupiter.params.provider.EnumSource.Mode;

				public class MyTest {
				    enum Color { RED, GREEN, BLUE }

				    @ParameterizedTest
				    @EnumSource(value = Color.class, from = "RED", to = "BLUE",
				            mode = Mode.EXCLUDE, names = {"RED"})
				    public void first(Color color) {
				    }

				    @ParameterizedTest
				    @EnumSource(value = Color.class, mode = Mode.EXCLUDE, names = {"BLUE"})
				    public void second(Color color) {
				    }
				}
				""");
		IMethod first= getMethod(cu, "first", "QColor;"); //$NON-NLS-1$ //$NON-NLS-2$
		IMethod second= getMethod(cu, "second", "QColor;"); //$NON-NLS-1$ //$NON-NLS-2$

		assertTrue(EnumSourceValidator.removeExcludeMode(first));

		String source= cu.getSource();
		assertTrue(source.contains("from = \"RED\"")); //$NON-NLS-1$
		assertTrue(source.contains("to = \"BLUE\"")); //$NON-NLS-1$
		assertTrue(source.contains("import org.junit.jupiter.params.provider.EnumSource.Mode;")); //$NON-NLS-1$
		assertFalse(EnumSourceValidator.isExcludeMode(first));
		assertTrue(EnumSourceValidator.isExcludeMode(second));
		assertCompiles(cu);
	}

	@Test
	public void testReincludeLastValueRemovesUnusedModeImport() throws Exception {
		ICompilationUnit cu= createCompilationUnit("""
				package test1;

				import org.junit.jupiter.params.ParameterizedTest;
				import org.junit.jupiter.params.provider.EnumSource;
				import org.junit.jupiter.params.provider.EnumSource.Mode;

				public class MyTest {
				    enum Color { RED, GREEN, BLUE }

				    @ParameterizedTest
				    @EnumSource(value = Color.class, from = "GREEN", to = "BLUE",
				            mode = Mode.EXCLUDE, names = {"GREEN"})
				    public void testWithEnum(Color color) {
				    }
				}
				""");
		IMethod method= getMethod(cu, "testWithEnum", "QColor;"); //$NON-NLS-1$ //$NON-NLS-2$

		assertTrue(EnumSourceValidator.removeValueFromExclusion(method, "GREEN")); //$NON-NLS-1$

		String source= cu.getSource();
		assertFalse(source.contains("import org.junit.jupiter.params.provider.EnumSource.Mode;")); //$NON-NLS-1$
		assertTrue(source.contains("from = \"GREEN\"")); //$NON-NLS-1$
		assertTrue(source.contains("to = \"BLUE\"")); //$NON-NLS-1$
		assertFalse(EnumSourceValidator.isExcludeMode(method));
		assertCompiles(cu);
	}


	@Test
	public void testReincludeSingleValuePreservesOtherExclusions() throws Exception {
		ICompilationUnit cu= createCompilationUnit("""
				package test1;

				import org.junit.jupiter.params.ParameterizedTest;
				import org.junit.jupiter.params.provider.EnumSource;

				public class MyTest {
				    enum Color { RED, GREEN, BLUE }

				    @ParameterizedTest
				    @EnumSource(value = Color.class, mode = EnumSource.Mode.EXCLUDE,
				            names = {"RED", "GREEN"})
				    public void testWithEnum(Color color) {
				    }
				}
				""");
		IMethod method= getMethod(cu, "testWithEnum", "QColor;"); //$NON-NLS-1$ //$NON-NLS-2$

		assertTrue(EnumSourceValidator.removeValueFromExclusion(method, "RED")); //$NON-NLS-1$

		assertEquals(List.of("GREEN"), EnumSourceValidator.getExcludedNames(method)); //$NON-NLS-1$
		assertTrue(EnumSourceValidator.isExcludeMode(method));
		assertCompiles(cu);
	}

	@Test
	public void testOverloadedMethodChangesOnlyExactBinding() throws Exception {
		ICompilationUnit cu= createCompilationUnit("""
				package test1;

				import org.junit.jupiter.params.ParameterizedTest;
				import org.junit.jupiter.params.provider.EnumSource;

				public class MyTest {
				    enum Color { RED, GREEN }
				    enum Shape { CIRCLE, SQUARE }

				    @ParameterizedTest
				    @EnumSource(Color.class)
				    public void testValue(Color color) {
				    }

				    @ParameterizedTest
				    @EnumSource(Shape.class)
				    public void testValue(Shape shape) {
				    }
				}
				""");
		IMethod colorMethod= getMethod(cu, "testValue", "QColor;"); //$NON-NLS-1$ //$NON-NLS-2$
		IMethod shapeMethod= getMethod(cu, "testValue", "QShape;"); //$NON-NLS-1$ //$NON-NLS-2$

		assertTrue(EnumSourceValidator.excludeEnumValue(shapeMethod, "CIRCLE")); //$NON-NLS-1$

		assertTrue(EnumSourceValidator.getExcludedNames(colorMethod).isEmpty());
		assertEquals(List.of("CIRCLE"), EnumSourceValidator.getExcludedNames(shapeMethod)); //$NON-NLS-1$
		assertCompiles(cu);
	}

	@Test
	public void testMultipleAndRepeatedArgumentSourcesAreRejected() throws Exception {
		ICompilationUnit cu= createCompilationUnit("""
				package test1;

				import org.junit.jupiter.params.ParameterizedTest;
				import org.junit.jupiter.params.provider.EnumSource;
				import org.junit.jupiter.params.provider.ValueSource;

				public class MyTest {
				    enum Color { RED, GREEN }

				    @ParameterizedTest
				    @EnumSource(Color.class)
				    @ValueSource(strings = "other")
				    public void mixed(Object value) {
				    }

				    @ParameterizedTest
				    @EnumSource(names = "RED")
				    @EnumSource(names = "GREEN")
				    public void repeated(Color color) {
				    }
				}
				""");
		IMethod mixed= getMethod(cu, "mixed", "QObject;"); //$NON-NLS-1$ //$NON-NLS-2$
		IMethod repeated= getMethod(cu, "repeated", "QColor;"); //$NON-NLS-1$ //$NON-NLS-2$

		assertFalse(EnumSourceValidator.canExcludeEnumValue(mixed, "RED")); //$NON-NLS-1$
		assertFalse(EnumSourceValidator.canExcludeEnumValue(repeated, "RED")); //$NON-NLS-1$
		assertCompiles(cu);
	}

	@Test
	public void testInvocationOrderHonorsRangeAndExclusions() throws Exception {
		ICompilationUnit cu= createCompilationUnit("""
				package test1;

				import org.junit.jupiter.params.ParameterizedTest;
				import org.junit.jupiter.params.provider.EnumSource;

				public class MyTest {
				    enum Color { RED, GREEN, BLUE, YELLOW }

				    @ParameterizedTest
				    @EnumSource(value = Color.class, from = "GREEN", to = "YELLOW",
				            mode = EnumSource.Mode.EXCLUDE, names = {"BLUE"})
				    public void testWithEnum(Color color) {
				    }
				}
				""");
		IMethod method= getMethod(cu, "testWithEnum", "QColor;"); //$NON-NLS-1$ //$NON-NLS-2$

		assertEquals("GREEN", EnumSourceValidator.getEnumConstantForInvocation(method, 1)); //$NON-NLS-1$
		assertEquals("YELLOW", EnumSourceValidator.getEnumConstantForInvocation(method, 2)); //$NON-NLS-1$
		assertNull(EnumSourceValidator.getEnumConstantForInvocation(method, 3));
	}

	@Test
	public void testMethodFinderUsesParameterTypesAndRejectsAmbiguity() throws Exception {
		ICompilationUnit cu= createCompilationUnit("""
				package test1;

				public class MyTest {
				    public void overloaded(String value) {
				    }

				    public void overloaded(int value) {
				    }

				    public void unique(long value) {
				    }
				}
				""");
		IType type= cu.getType("MyTest"); //$NON-NLS-1$

		IMethod stringMethod= TestMethodFinder.findMethod(
				type, "overloaded", new String[] { "java.lang.String" }); //$NON-NLS-1$ //$NON-NLS-2$
		assertNotNull(stringMethod);
		assertEquals("QString;", stringMethod.getParameterTypes()[0]); //$NON-NLS-1$
		assertNull(TestMethodFinder.findMethod(type, "overloaded", null)); //$NON-NLS-1$
		assertEquals("unique", TestMethodFinder.findMethod(type, "unique", null).getElementName()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private ICompilationUnit createCompilationUnit(String source) throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null); //$NON-NLS-1$
		return pack.createCompilationUnit("MyTest.java", source, false, null); //$NON-NLS-1$
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

	private static int countOccurrences(String text, String pattern) {
		int count= 0;
		int index= 0;
		while ((index= text.indexOf(pattern, index)) >= 0) {
			count++;
			index += pattern.length();
		}
		return count;
	}
}
