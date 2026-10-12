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

import static org.eclipse.jdt.junit.tests.EnumSourceTestSupport.assertCompiles;
import static org.eclipse.jdt.junit.tests.EnumSourceTestSupport.assertExcludeMode;
import static org.eclipse.jdt.junit.tests.EnumSourceTestSupport.assertExcludedNames;
import static org.eclipse.jdt.junit.tests.EnumSourceTestSupport.assertExclusionTarget;
import static org.eclipse.jdt.junit.tests.EnumSourceTestSupport.assertFilterRemoved;
import static org.eclipse.jdt.junit.tests.EnumSourceTestSupport.assertNoExclusionTarget;
import static org.eclipse.jdt.junit.tests.EnumSourceTestSupport.getMethod;
import static org.eclipse.jdt.junit.tests.EnumSourceTestSupport.invocation;
import static org.eclipse.jdt.junit.tests.EnumSourceTestSupport.methodContext;
import static org.eclipse.jdt.junit.tests.EnumSourceTestSupport.sourceContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

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
	public void testAppendExclusionExpandsStringLiteral() throws Exception {
		assertAppendExclusionExpandsSingleName("\"RED\""); //$NON-NLS-1$
	}

	@Test
	public void testAppendExclusionExpandsConstantExpression() throws Exception {
		assertAppendExclusionExpandsSingleName("\"R\" + \"ED\""); //$NON-NLS-1$
	}

	private void assertAppendExclusionExpandsSingleName(String nameExpression) throws Exception {
		// Both named tests exercise single-value shorthand without an ArrayInitializer.
		IMethod method= createTest("""
				@EnumSource(value = Color.class, from = "RED", to = "BLUE",
				    mode = EnumSource.Mode.EXCLUDE, names = %s)
				""".formatted(nameExpression));
		ICompilationUnit cu= method.getCompilationUnit();
		assertExcludedNames(method, List.of("RED")); //$NON-NLS-1$
		assertExclusionTarget(method, 1, "GREEN"); //$NON-NLS-1$

		assertTrue("Expected excludeEnumValue(GREEN) to succeed for " + methodContext(method), //$NON-NLS-1$
				EnumSourceValidator.excludeEnumValue(method, "GREEN")); //$NON-NLS-1$

		assertExcludedNames(method, List.of("RED", "GREEN")); //$NON-NLS-1$ //$NON-NLS-2$
		assertExcludeMode(method);
		assertNoExclusionTarget(method, 1);
		String source= cu.getSource();
		String expectedNames= "names={" + nameExpression + ",\"GREEN\"}"; //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Expected the original name expression and appended GREEN in source: " + source, //$NON-NLS-1$
				source.replaceAll("\\s+", "").contains(expectedNames.replaceAll("\\s+", ""))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		assertTrue("Expected the RED lower bound to be preserved in source: " + source, source.contains("from = \"RED\"")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Expected the BLUE upper bound to be preserved in source: " + source, source.contains("to = \"BLUE\"")); //$NON-NLS-1$ //$NON-NLS-2$
		assertCompiles(cu);

		assertFalse("Expected excludeEnumValue(GREEN) to be rejected for " + methodContext(method), //$NON-NLS-1$
				EnumSourceValidator.excludeEnumValue(method, "GREEN")); //$NON-NLS-1$
		assertEquals("Expected repeating the same exclusion to leave the source unchanged", source, cu.getSource()); //$NON-NLS-1$
		assertTrue("Expected removeValueFromExclusion(RED) to succeed for " + methodContext(method), //$NON-NLS-1$
				EnumSourceValidator.removeValueFromExclusion(method, "RED")); //$NON-NLS-1$
		assertExcludedNames(method, List.of("GREEN")); //$NON-NLS-1$
		assertExclusionTarget(method, 1, "RED"); //$NON-NLS-1$
		assertExclusionTarget(method, 2, "BLUE"); //$NON-NLS-1$
		assertNoExclusionTarget(method, 3);
		assertCompiles(cu);
	}

	@Test
	public void testReincludeOneWhenAllValuesExcluded() throws Exception {
		IMethod method= createTest("""
				@EnumSource(value = Color.class, mode = EnumSource.Mode.EXCLUDE,
				    names = { "RED", "GREEN", "BLUE" })
				""");

		assertExcludeMode(method);
		assertExcludedNames(method, List.of("RED", "GREEN", "BLUE")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertNoExclusionTarget(method, 1);

		assertTrue("Expected removeValueFromExclusion(GREEN) to succeed for " + methodContext(method), //$NON-NLS-1$
				EnumSourceValidator.removeValueFromExclusion(method, "GREEN")); //$NON-NLS-1$
		assertExcludedNames(method, List.of("RED", "BLUE")); //$NON-NLS-1$ //$NON-NLS-2$
		// GREEN is runnable again but is the last value, so it cannot be excluded.
		assertNoExclusionTarget(method, 1);
		assertNoExclusionTarget(method, 2);
		assertCompiles(method.getCompilationUnit());
	}

	@Test
	public void testReincludeLastExcludedValueInRange() throws Exception {
		IMethod method= createTest("""
				@EnumSource(value = Color.class, from = "GREEN", to = "GREEN",
				    mode = EnumSource.Mode.EXCLUDE, names = "GREEN")
				""");

		assertExcludeMode(method);
		assertNoExclusionTarget(method, 1);
		assertTrue("Expected removeValueFromExclusion(GREEN) to succeed for " + methodContext(method), //$NON-NLS-1$
				EnumSourceValidator.removeValueFromExclusion(method, "GREEN")); //$NON-NLS-1$
		assertFilterRemoved(method);
		String source= method.getCompilationUnit().getSource();
		assertTrue("Expected the GREEN lower bound to remain in " + sourceContext(method.getCompilationUnit()), //$NON-NLS-1$
				source.contains("from = \"GREEN\"")); //$NON-NLS-1$
		assertTrue("Expected the GREEN upper bound to remain in " + sourceContext(method.getCompilationUnit()), //$NON-NLS-1$
				source.contains("to = \"GREEN\"")); //$NON-NLS-1$
		// GREEN is runnable again but is the last value, so it cannot be excluded.
		assertNoExclusionTarget(method, 1);
		assertNoExclusionTarget(method, 2);
		assertCompiles(method.getCompilationUnit());
	}

	@Test
	public void testReincludeAllWhenRangeFullyExcluded() throws Exception {
		IMethod method= createTest("""
				@EnumSource(value = Color.class, from = "GREEN", to = "BLUE",
				    mode = EnumSource.Mode.EXCLUDE, names = { "GREEN", "BLUE" })
				""");

		assertExcludeMode(method);
		assertNoExclusionTarget(method, 1);
		assertTrue("Expected removeExcludeMode to succeed for " + methodContext(method), //$NON-NLS-1$
				EnumSourceValidator.removeExcludeMode(method));
		assertFilterRemoved(method);
		assertExcludedNames(method, List.of());
		assertExclusionTarget(method, 1, "GREEN"); //$NON-NLS-1$
		assertExclusionTarget(method, 2, "BLUE"); //$NON-NLS-1$
		assertNoExclusionTarget(method, 3);
		assertCompiles(method.getCompilationUnit());
	}

	@Test
	public void testMatchAnyFilterIsNotEditable() throws Exception {
		assertRegexFilterNotEditable("MATCH_ANY"); //$NON-NLS-1$
	}

	@Test
	public void testMatchAllFilterIsNotEditable() throws Exception {
		assertRegexFilterNotEditable("MATCH_ALL"); //$NON-NLS-1$
	}

	@Test
	public void testMatchNoneFilterIsNotEditable() throws Exception {
		assertRegexFilterNotEditable("MATCH_NONE"); //$NON-NLS-1$
	}

	private void assertRegexFilterNotEditable(String mode) throws Exception {
		IMethod method= createTest("""
				@EnumSource(value = Color.class, mode = EnumSource.Mode.%s, names = "R.*")
				""".formatted(mode));
		assertNotEditable(method);
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
		assertActionEnabled("[test-template-invocation:#1]"); //$NON-NLS-1$
		assertActionEnabled("[test-template-invocation:#3]"); //$NON-NLS-1$
	}

	@Test
	public void testNonDynamicInvocationIsNotEditable() throws Exception {
		createTest("@EnumSource(Color.class)"); //$NON-NLS-1$
		assertActionDisabled("[test-template-invocation:#1]", false); //$NON-NLS-1$
	}

	@Test
	public void testInvocationMappingRejectsInvalidIndices() throws Exception {
		createTest("@EnumSource(Color.class)"); //$NON-NLS-1$
		assertActionDisabled(null);
		for (String id : List.of("", "[engine:junit-jupiter]", //$NON-NLS-1$ //$NON-NLS-2$
				"[test-template-invocation:#0]", "[test-template-invocation:#4]", //$NON-NLS-1$ //$NON-NLS-2$
				"[test-template-invocation:#-1]", "[test-template-invocation:#2147483648]", //$NON-NLS-1$ //$NON-NLS-2$
				"[test-template-invocation:#999999999999999999999999]")) { //$NON-NLS-1$
			assertActionDisabled(id);
		}
	}

	@Test
	public void testInvocationMappingChecksLastMatchingSegment() throws Exception {
		createTest("@EnumSource(Color.class)"); //$NON-NLS-1$
		// Synthetic IDs pin the existing last-match behavior, not a new supported source kind.
		assertActionEnabled("[test-template-invocation:#99]/[test-template-invocation:#2]"); //$NON-NLS-1$
		assertActionDisabled("[test-template-invocation:#2]/[test-template-invocation:#99]"); //$NON-NLS-1$
		assertActionDisabled("[test-template-invocation:#2]/[test-template-invocation:#0]"); //$NON-NLS-1$
	}

	@Test
	public void testActionDoesNotRemapAnAlreadyExcludedValue() throws Exception {
		IMethod method= createTest("@EnumSource(Color.class)"); //$NON-NLS-1$
		ExcludeParameterValueAction action= new ExcludeParameterValueAction();
		action.update(invocation(method, 2));
		assertTrue("Expected GREEN at invocation index 2 to be eligible for exclusion in " + methodContext(method), action.isEnabled()); //$NON-NLS-1$

		assertTrue("Expected excludeEnumValue(GREEN) to succeed for " + methodContext(method), //$NON-NLS-1$
				EnumSourceValidator.excludeEnumValue(method, "GREEN")); //$NON-NLS-1$
		String source= method.getCompilationUnit().getSource();
		action.run();

		assertEquals("Expected no changes when the selected GREEN value is already excluded", source, method.getCompilationUnit().getSource()); //$NON-NLS-1$
		assertExcludedNames(method, List.of("GREEN")); //$NON-NLS-1$
		assertFalse("Expected no changes when the selected GREEN value is already excluded; the action must be disabled", action.isEnabled()); //$NON-NLS-1$
		assertCompiles(method.getCompilationUnit());
	}

	@Test
	public void testActionRevalidatesSourceAfterMenuWasOpened() throws Exception {
		IMethod method= createTest("@EnumSource(Color.class)"); //$NON-NLS-1$
		ExcludeParameterValueAction action= new ExcludeParameterValueAction();
		action.update(invocation(method, 2));
		assertTrue("Expected GREEN at invocation index 2 to be eligible for exclusion in " + methodContext(method), action.isEnabled()); //$NON-NLS-1$

		IMethod updatedMethod= createTest("""
				@EnumSource(value = Color.class, mode = EnumSource.Mode.MATCH_ANY, names = "R.*")
				""");
		String source= updatedMethod.getCompilationUnit().getSource();
		action.run();

		assertEquals("Expected no changes after the source becomes unsupported", source, updatedMethod.getCompilationUnit().getSource()); //$NON-NLS-1$
		assertFalse("Expected no changes after the source becomes unsupported; the action must be disabled", action.isEnabled()); //$NON-NLS-1$
		assertCompiles(updatedMethod.getCompilationUnit());
	}

	@Test
	public void testClearingSelectionDropsExclusionTarget() throws Exception {
		IMethod method= createTest("@EnumSource(Color.class)"); //$NON-NLS-1$
		ExcludeParameterValueAction action= new ExcludeParameterValueAction();
		action.update(invocation(method, 2));
		assertTrue("Expected GREEN at invocation index 2 to be eligible for exclusion in " + methodContext(method), action.isEnabled()); //$NON-NLS-1$

		action.update(null);
		String source= method.getCompilationUnit().getSource();
		action.run();

		assertEquals("Expected no changes after clearing the selection", source, method.getCompilationUnit().getSource()); //$NON-NLS-1$
		assertFalse("Expected no changes after clearing the selection; the action must be disabled", action.isEnabled()); //$NON-NLS-1$
	}

	private IMethod createTest(String enumSource) throws Exception {
		ICompilationUnit cu= EnumSourceTestSupport.createCompilationUnit(fSourceFolder, "test1", "MyTest.java", """
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
				""".formatted(enumSource), true);
		return getMethod(cu, "testWithEnum", "QColor;"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static void assertNotEditable(IMethod method) throws Exception {
		String original= method.getCompilationUnit().getSource();
		assertExcludedNames(method, List.of());
		assertNoExclusionTarget(method, 1);
		assertFalse("Expected excludeEnumValue(RED) to be rejected for " + methodContext(method), //$NON-NLS-1$
				EnumSourceValidator.excludeEnumValue(method, "RED")); //$NON-NLS-1$
		assertFalse("Expected removeValueFromExclusion(GREEN) to be rejected for " + methodContext(method), //$NON-NLS-1$
				EnumSourceValidator.removeValueFromExclusion(method, "GREEN")); //$NON-NLS-1$
		assertFalse("Expected removeExcludeMode to be rejected for " + methodContext(method), //$NON-NLS-1$
				EnumSourceValidator.removeExcludeMode(method));
		assertEquals("Expected unsupported source to remain unchanged", original, method.getCompilationUnit().getSource()); //$NON-NLS-1$
	}

	private void assertActionEnabled(String uniqueId) {
		assertTrue("Expected enum exclusion to be enabled for unique ID: " + uniqueId, isActionEnabled(uniqueId)); //$NON-NLS-1$
	}

	private void assertActionDisabled(String uniqueId) {
		assertActionDisabled(uniqueId, true);
	}

	private void assertActionDisabled(String uniqueId, boolean dynamic) {
		assertFalse("Expected enum exclusion to be disabled for unique ID: " + uniqueId + " (dynamic=" + dynamic + ")", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				isActionEnabled(uniqueId, dynamic));
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
}
