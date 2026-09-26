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

import static org.eclipse.jdt.junit.tests.EnumSourceTestSupport.assertExcludeMode;
import static org.eclipse.jdt.junit.tests.EnumSourceTestSupport.assertFilterRemoved;
import static org.eclipse.jdt.junit.tests.EnumSourceTestSupport.enumConstantForInvocation;
import static org.eclipse.jdt.junit.tests.EnumSourceTestSupport.invocation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.junit.model.TestCaseElement;
import org.eclipse.jdt.internal.junit.model.TestRunSession;
import org.eclipse.jdt.internal.junit.model.TestSuiteElement;
import org.eclipse.jdt.internal.junit.ui.EnumSourceValidator;
import org.eclipse.jdt.internal.junit.ui.ExcludeParameterValueAction;

import org.eclipse.jdt.ui.tests.core.rules.Java1d8ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

/**
 * Regression tests for unsupported sources, empty supported sources and invocation bounds.
 */
public class EnumSourceValidationTest {

	@Rule
	public ProjectTestSetup projectSetup= new Java1d8ProjectTestSetup();

	private IJavaProject fJProject;
	private IPackageFragmentRoot fSourceFolder;

	@Before
	public void setUp() throws Exception {
		fJProject= projectSetup.getProject();
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject, "src"); //$NON-NLS-1$
		JavaProjectHelper.addRTJar(fJProject);
		JavaProjectHelper.addToClasspath(fJProject, JavaCore.newContainerEntry(JUnitCore.JUNIT5_CONTAINER_PATH));
		JavaProjectHelper.set18CompilerOptions(fJProject);
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject, projectSetup.getDefaultClasspath());
	}

	@Test
	public void testAppendExclusionExpandsSingleName() throws Exception {
		// Array-valued annotation members can omit braces for one element. These
		// expressions exercise the non-ArrayInitializer branch when adding a value.
		for (String nameExpression : List.of("\"RED\"", "\"R\" + \"ED\"")) { //$NON-NLS-1$ //$NON-NLS-2$
			IMethod method= createTest("""
					@EnumSource(value = Color.class, from = "RED", to = "GREEN",
					    mode = EnumSource.Mode.EXCLUDE, names = %s)
					""".formatted(nameExpression));
			ICompilationUnit cu= method.getCompilationUnit();
			assertEquals(List.of("RED"), EnumSourceValidator.getExcludedNames(method)); //$NON-NLS-1$
			assertEquals("GREEN", enumConstantForInvocation(method, 1)); //$NON-NLS-1$

			assertTrue(EnumSourceValidator.excludeEnumValue(method, "GREEN")); //$NON-NLS-1$

			assertEquals(List.of("RED", "GREEN"), EnumSourceValidator.getExcludedNames(method)); //$NON-NLS-1$ //$NON-NLS-2$
			assertExcludeMode(method);
			assertNull(enumConstantForInvocation(method, 1));
			String source= cu.getSource();
			String expectedNames= "names={" + nameExpression + ",\"GREEN\"}"; //$NON-NLS-1$ //$NON-NLS-2$
			assertTrue(source.replaceAll("\\s+", "").contains(expectedNames.replaceAll("\\s+", ""))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			assertTrue(source.contains("from = \"RED\"")); //$NON-NLS-1$
			assertTrue(source.contains("to = \"GREEN\"")); //$NON-NLS-1$
			assertCompiles(cu);

			assertFalse(EnumSourceValidator.excludeEnumValue(method, "GREEN")); //$NON-NLS-1$
			assertEquals(source, cu.getSource());
			assertTrue(EnumSourceValidator.removeValueFromExclusion(method, "RED")); //$NON-NLS-1$
			assertEquals(List.of("GREEN"), EnumSourceValidator.getExcludedNames(method)); //$NON-NLS-1$
			assertEquals("RED", enumConstantForInvocation(method, 1)); //$NON-NLS-1$
			assertNull(enumConstantForInvocation(method, 2));
			assertCompiles(cu);
		}
	}

	@Test
	public void testReincludeOneWhenAllValuesExcluded() throws Exception {
		IMethod method= createTest("""
				@EnumSource(value = Color.class, mode = EnumSource.Mode.EXCLUDE,
				    names = { "RED", "GREEN", "BLUE" })
				""");

		assertExcludeMode(method);
		assertEquals(List.of("RED", "GREEN", "BLUE"), EnumSourceValidator.getExcludedNames(method)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertNull(enumConstantForInvocation(method, 1));

		assertTrue(EnumSourceValidator.removeValueFromExclusion(method, "GREEN")); //$NON-NLS-1$
		assertEquals(List.of("RED", "BLUE"), EnumSourceValidator.getExcludedNames(method)); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("GREEN", enumConstantForInvocation(method, 1)); //$NON-NLS-1$
		assertNull(enumConstantForInvocation(method, 2));
		assertCompiles(method.getCompilationUnit());
	}

	@Test
	public void testReincludeLastExcludedValueInRange() throws Exception {
		IMethod method= createTest("""
				@EnumSource(value = Color.class, from = "GREEN", to = "GREEN",
				    mode = EnumSource.Mode.EXCLUDE, names = "GREEN")
				""");

		assertExcludeMode(method);
		assertNull(enumConstantForInvocation(method, 1));
		assertTrue(EnumSourceValidator.removeValueFromExclusion(method, "GREEN")); //$NON-NLS-1$
		assertFilterRemoved(method);
		assertEquals("GREEN", enumConstantForInvocation(method, 1)); //$NON-NLS-1$
		assertNull(enumConstantForInvocation(method, 2));
		assertCompiles(method.getCompilationUnit());
	}

	@Test
	public void testReincludeAllWhenRangeFullyExcluded() throws Exception {
		IMethod method= createTest("""
				@EnumSource(value = Color.class, from = "GREEN", to = "BLUE",
				    mode = EnumSource.Mode.EXCLUDE, names = { "GREEN", "BLUE" })
				""");

		assertExcludeMode(method);
		assertNull(enumConstantForInvocation(method, 1));
		assertTrue(EnumSourceValidator.removeExcludeMode(method));
		assertFilterRemoved(method);
		assertTrue(EnumSourceValidator.getExcludedNames(method).isEmpty());
		assertEquals("GREEN", enumConstantForInvocation(method, 1)); //$NON-NLS-1$
		assertEquals("BLUE", enumConstantForInvocation(method, 2)); //$NON-NLS-1$
		assertNull(enumConstantForInvocation(method, 3));
		assertCompiles(method.getCompilationUnit());
	}

	@Test
	public void testRegexFiltersAreNotEditable() throws Exception {
		for (String mode : List.of("MATCH_ANY", "MATCH_ALL", "MATCH_NONE")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			IMethod method= createTest("""
					@EnumSource(value = Color.class, mode = EnumSource.Mode.%s, names = "R.*")
					""".formatted(mode));
			assertNotEditable(method);
		}
	}

	@Test
	public void testInvalidRangeIsNotEditable() throws Exception {
		IMethod method= createTest("""
				@EnumSource(value = Color.class, from = "BLUE", to = "RED",
				    mode = EnumSource.Mode.EXCLUDE, names = "GREEN")
				""");
		assertNotEditable(method);
	}

	@Test
	public void testInvocationMappingAcceptsBoundaryIndices() throws Exception {
		createTest("@EnumSource(Color.class)"); //$NON-NLS-1$
		assertTrue(isActionEnabled("[test-template-invocation:#1]")); //$NON-NLS-1$
		assertTrue(isActionEnabled("[test-template-invocation:#3]")); //$NON-NLS-1$
	}

	@Test
	public void testNonDynamicInvocationIsNotEditable() throws Exception {
		createTest("@EnumSource(Color.class)"); //$NON-NLS-1$
		assertFalse(isActionEnabled("[test-template-invocation:#1]", false)); //$NON-NLS-1$
	}

	@Test
	public void testInvocationMappingRejectsInvalidIndices() throws Exception {
		createTest("@EnumSource(Color.class)"); //$NON-NLS-1$
		assertFalse(isActionEnabled(null));
		for (String id : List.of("", "[engine:junit-jupiter]", //$NON-NLS-1$ //$NON-NLS-2$
				"[test-template-invocation:#0]", "[test-template-invocation:#4]", //$NON-NLS-1$ //$NON-NLS-2$
				"[test-template-invocation:#-1]", "[test-template-invocation:#2147483648]", //$NON-NLS-1$ //$NON-NLS-2$
				"[test-template-invocation:#999999999999999999999999]")) { //$NON-NLS-1$
			assertFalse(id, isActionEnabled(id));
		}
	}

	@Test
	public void testInvocationMappingChecksLastMatchingSegment() throws Exception {
		createTest("@EnumSource(Color.class)"); //$NON-NLS-1$
		// Synthetic IDs pin the existing last-match behavior, not a new supported source kind.
		assertTrue(isActionEnabled("[test-template-invocation:#99]/[test-template-invocation:#2]")); //$NON-NLS-1$
		assertFalse(isActionEnabled("[test-template-invocation:#2]/[test-template-invocation:#99]")); //$NON-NLS-1$
		assertFalse(isActionEnabled("[test-template-invocation:#2]/[test-template-invocation:#0]")); //$NON-NLS-1$
	}

	@Test
	public void testActionDoesNotRemapAnAlreadyExcludedValue() throws Exception {
		IMethod method= createTest("@EnumSource(Color.class)"); //$NON-NLS-1$
		ExcludeParameterValueAction action= new ExcludeParameterValueAction();
		action.update(invocation(method, 2));
		assertTrue(action.isEnabled());

		assertTrue(EnumSourceValidator.excludeEnumValue(method, "GREEN")); //$NON-NLS-1$
		String source= method.getCompilationUnit().getSource();
		action.run();

		assertEquals(source, method.getCompilationUnit().getSource());
		assertEquals(List.of("GREEN"), EnumSourceValidator.getExcludedNames(method)); //$NON-NLS-1$
		assertFalse(action.isEnabled());
		assertCompiles(method.getCompilationUnit());
	}

	@Test
	public void testActionRevalidatesSourceAfterMenuWasOpened() throws Exception {
		IMethod method= createTest("@EnumSource(Color.class)"); //$NON-NLS-1$
		ExcludeParameterValueAction action= new ExcludeParameterValueAction();
		action.update(invocation(method, 2));
		assertTrue(action.isEnabled());

		IMethod updatedMethod= createTest("""
				@EnumSource(value = Color.class, mode = EnumSource.Mode.MATCH_ANY, names = "R.*")
				""");
		String source= updatedMethod.getCompilationUnit().getSource();
		action.run();

		assertEquals(source, updatedMethod.getCompilationUnit().getSource());
		assertFalse(action.isEnabled());
		assertCompiles(updatedMethod.getCompilationUnit());
	}

	@Test
	public void testClearingSelectionDropsExclusionTarget() throws Exception {
		IMethod method= createTest("@EnumSource(Color.class)"); //$NON-NLS-1$
		ExcludeParameterValueAction action= new ExcludeParameterValueAction();
		action.update(invocation(method, 2));
		assertTrue(action.isEnabled());

		action.update(null);
		String source= method.getCompilationUnit().getSource();
		action.run();

		assertEquals(source, method.getCompilationUnit().getSource());
		assertFalse(action.isEnabled());
	}

	private IMethod createTest(String enumSource) throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null); //$NON-NLS-1$
		ICompilationUnit cu= pack.createCompilationUnit("MyTest.java", """
				package test1;

				import org.junit.jupiter.params.ParameterizedTest;
				import org.junit.jupiter.params.provider.EnumSource;

				public class MyTest {
				    enum Color { RED, GREEN, BLUE }

				    @ParameterizedTest
				    %s
				    public void testWithEnum(Color color) {
				    }
				}
				""".formatted(enumSource), true, null);
		assertCompiles(cu);
		return cu.getType("MyTest").getMethod("testWithEnum", new String[] { "QColor;" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	private static void assertNotEditable(IMethod method) throws Exception {
		String original= method.getCompilationUnit().getSource();
		assertTrue(EnumSourceValidator.getExcludedNames(method).isEmpty());
		assertNull(enumConstantForInvocation(method, 1));
		assertFalse(EnumSourceValidator.excludeEnumValue(method, "RED")); //$NON-NLS-1$
		assertFalse(EnumSourceValidator.removeValueFromExclusion(method, "GREEN")); //$NON-NLS-1$
		assertFalse(EnumSourceValidator.removeExcludeMode(method));
		assertEquals(original, method.getCompilationUnit().getSource());
	}

	private boolean isActionEnabled(String uniqueId) {
		return isActionEnabled(uniqueId, true);
	}

	private boolean isActionEnabled(String uniqueId, boolean dynamic) {
		TestRunSession session= new TestRunSession("EnumSource validation run", fJProject); //$NON-NLS-1$
		TestSuiteElement suite= new TestSuiteElement(session.getTestRoot(), "suite", //$NON-NLS-1$
				"testWithEnum(test1.MyTest$Color)(test1.MyTest)", 3, null, //$NON-NLS-1$
				new String[] { "test1.MyTest$Color" }, null); //$NON-NLS-1$
		TestCaseElement testCase= new TestCaseElement(suite, "case", //$NON-NLS-1$
				"custom(test1.MyTest)", "custom", dynamic, null, uniqueId); //$NON-NLS-1$ //$NON-NLS-2$
		ExcludeParameterValueAction action= new ExcludeParameterValueAction();
		action.update(testCase);
		return action.isEnabled();
	}

	private static void assertCompiles(ICompilationUnit cu) {
		ASTParser parser= ASTParser.newParser(AST.getJLSLatest());
		parser.setSource(cu);
		parser.setResolveBindings(true);
		CompilationUnit root= (CompilationUnit) parser.createAST(null);
		String errors= Arrays.stream(root.getProblems()).filter(IProblem::isError)
				.map(IProblem::getMessage).collect(Collectors.joining("\n")); //$NON-NLS-1$
		assertEquals(errors, "", errors); //$NON-NLS-1$
	}
}
