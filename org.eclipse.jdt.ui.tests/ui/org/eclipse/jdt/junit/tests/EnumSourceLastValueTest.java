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

import static org.eclipse.jdt.junit.tests.EnumSourceTestSupport.assertExcludedNames;
import static org.eclipse.jdt.junit.tests.EnumSourceTestSupport.assertNoExclusionTarget;
import static org.eclipse.jdt.junit.tests.EnumSourceTestSupport.getMethod;
import static org.eclipse.jdt.junit.tests.EnumSourceTestSupport.invocation;
import static org.eclipse.jdt.junit.tests.EnumSourceTestSupport.methodContext;
import static org.eclipse.jdt.junit.tests.EnumSourceTestSupport.sourceContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;

import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.junit.TestRunListener;
import org.eclipse.jdt.junit.model.ITestCaseElement;
import org.eclipse.jdt.junit.model.ITestElement.Result;
import org.eclipse.jdt.junit.model.ITestRunSession;
import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;
import org.eclipse.jdt.internal.junit.ui.EnumSourceValidator;
import org.eclipse.jdt.internal.junit.ui.ExcludeParameterValueAction;

/** Tests both exclusion safety and actual execution of the remaining Jupiter invocations. */
public class EnumSourceLastValueTest extends AbstractTestRunListenerTest {

	@Override
	@Before
	public void setUp() throws Exception {
		fProject= JavaProjectHelper.createJavaProject("EnumSourceLastValueTest", "bin"); //$NON-NLS-1$ //$NON-NLS-2$
		JavaProjectHelper.addToClasspath(fProject, JavaCore.newContainerEntry(JUnitCore.JUNIT5_CONTAINER_PATH));
		JavaProjectHelper.addRTJar18(fProject);
		JavaProjectHelper.set18CompilerOptions(fProject);
	}

	@Test
	public void testAllFilterFormsKeepOneRunnableValue() throws Exception {
		IType type= createTestType("""
				package pack;
				import static org.junit.jupiter.api.Assertions.assertEquals;
				import org.junit.jupiter.params.ParameterizedTest;
				import org.junit.jupiter.params.provider.EnumSource;
				public class LastValueTest {
				    enum Color { RED, GREEN, BLUE }
				    enum Single { ONLY }

				    @ParameterizedTest
				    @EnumSource(Color.class)
				    void unfiltered(Color color) { assertEquals(Color.BLUE, color); }

				    @ParameterizedTest
				    @EnumSource(value = Color.class, names = { "GREEN", "BLUE" })
				    void included(Color color) { assertEquals(Color.BLUE, color); }

				    @ParameterizedTest
				    @EnumSource(value = Color.class, mode = EnumSource.Mode.EXCLUDE, names = "RED")
				    void excluded(Color color) { assertEquals(Color.BLUE, color); }

				    @ParameterizedTest
				    @EnumSource(value = Color.class, from = "GREEN", to = "BLUE")
				    void ranged(Color color) { assertEquals(Color.BLUE, color); }

				    @ParameterizedTest
				    @EnumSource(Single.class)
				    void singleton(Single single) { assertEquals(Single.ONLY, single); }
				}
				""");

		IMethod unfiltered= getMethod(type.getCompilationUnit(), "unfiltered", "QColor;"); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Expected exclusion of RED from three values in " + methodContext(unfiltered), EnumSourceValidator.excludeEnumValue(unfiltered, "RED")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTwoToOne(unfiltered);
		assertTwoToOne(getMethod(type.getCompilationUnit(), "included", "QColor;")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTwoToOne(getMethod(type.getCompilationUnit(), "excluded", "QColor;")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTwoToOne(getMethod(type.getCompilationUnit(), "ranged", "QColor;")); //$NON-NLS-1$ //$NON-NLS-2$
		assertLastValueProtected(getMethod(type.getCompilationUnit(), "singleton", "QSingle;"), "ONLY"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		// Compilation alone cannot detect an empty parameterized source. Launch Jupiter
		// and verify that all five methods really execute their sole remaining value.
		assertSuccessfulRun(type, 5);
	}

	@Test
	public void testSingleValueRangeWithEmptyNamesIsProtected() throws Exception {
		IType type= createTestType("""
				package pack;
				import org.junit.jupiter.params.ParameterizedTest;
				import org.junit.jupiter.params.provider.EnumSource;
				public class LastValueTest {
				    enum Color { RED, GREEN, BLUE }
				    @ParameterizedTest
				    @EnumSource(value = Color.class, from = "GREEN", to = "GREEN", names = {})
				    void ranged(Color color) { }
				}
				""");
		assertLastValueProtected(getMethod(type.getCompilationUnit(), "ranged", "QColor;"), "GREEN"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	@Test
	public void testActionRevalidatesLastValueAfterMenuWasOpened() throws Exception {
		IType type= createTestType("""
				package pack;
				import org.junit.jupiter.params.ParameterizedTest;
				import org.junit.jupiter.params.provider.EnumSource;
				public class LastValueTest {
				    enum Color { RED, GREEN, BLUE }
				    @ParameterizedTest
				    @EnumSource(value = Color.class, mode = EnumSource.Mode.EXCLUDE, names = "RED")
				    void excluded(Color color) { }
				}
				""");
		IMethod method= getMethod(type.getCompilationUnit(), "excluded", "QColor;"); //$NON-NLS-1$ //$NON-NLS-2$
		ExcludeParameterValueAction action= new ExcludeParameterValueAction();
		action.update(invocation(method, 2));
		assertTrue("Expected BLUE at invocation index 2 to be excludable while GREEN is present in " + methodContext(method), action.isEnabled()); //$NON-NLS-1$

		assertTrue("Expected exclusion of GREEN to leave BLUE in " + methodContext(method), EnumSourceValidator.excludeEnumValue(method, "GREEN")); //$NON-NLS-1$ //$NON-NLS-2$
		String source= method.getCompilationUnit().getSource();
		action.run();

		assertEquals("Expected the stale action not to remove the last value", source, method.getCompilationUnit().getSource()); //$NON-NLS-1$
		assertFalse("Expected the stale action for BLUE at invocation index 2 to become disabled in " + methodContext(method), action.isEnabled()); //$NON-NLS-1$
		assertLastValueProtected(method, "BLUE"); //$NON-NLS-1$
	}

	@Test
	public void testFullyExcludedSourceCanBeRepairedAndRun() throws Exception {
		IType type= createTestType("""
				package pack;
				import static org.junit.jupiter.api.Assertions.assertEquals;
				import org.junit.jupiter.params.ParameterizedTest;
				import org.junit.jupiter.params.provider.EnumSource;
				public class LastValueTest {
				    enum Color { RED, GREEN, BLUE }
				    @ParameterizedTest
				    @EnumSource(value = Color.class, mode = EnumSource.Mode.EXCLUDE,
				        names = { "RED", "GREEN", "BLUE" })
				    void repaired(Color color) { assertEquals(Color.GREEN, color); }
				}
				""");
		IMethod method= getMethod(type.getCompilationUnit(), "repaired", "QColor;"); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Expected re-inclusion of GREEN into the manually emptied source " + methodContext(method), //$NON-NLS-1$
				EnumSourceValidator.removeValueFromExclusion(method, "GREEN")); //$NON-NLS-1$
		assertExcludedNames(method, List.of("RED", "BLUE")); //$NON-NLS-1$ //$NON-NLS-2$
		assertLastValueProtected(method, "GREEN"); //$NON-NLS-1$
		assertSuccessfulRun(type, 1);
	}

	private IType createTestType(String source) throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fProject, "src"); //$NON-NLS-1$
		ICompilationUnit cu= EnumSourceTestSupport.createCompilationUnit(sourceFolder, "pack", "LastValueTest.java", source, true); //$NON-NLS-1$ //$NON-NLS-2$
		IType type= cu.findPrimaryType();
		assertNotNull("Expected the launch type in " + sourceContext(cu), type); //$NON-NLS-1$
		buildTestCase(type);
		return type;
	}

	private static void assertTwoToOne(IMethod method) throws Exception {
		ExcludeParameterValueAction action= new ExcludeParameterValueAction();
		action.update(invocation(method, 1));
		assertTrue("Expected exclusion to be offered with two values for " + methodContext(method), action.isEnabled()); //$NON-NLS-1$
		assertTrue("Expected two-to-one exclusion for " + methodContext(method), //$NON-NLS-1$
				EnumSourceValidator.excludeEnumValue(method, "GREEN")); //$NON-NLS-1$
		assertLastValueProtected(method, "BLUE"); //$NON-NLS-1$
	}

	private static void assertLastValueProtected(IMethod method, String value) throws Exception {
		String source= method.getCompilationUnit().getSource();
		// The resolver returns an editable exclusion target, not every runnable value.
		assertNoExclusionTarget(method, 1);
		ExcludeParameterValueAction action= new ExcludeParameterValueAction();
		action.update(invocation(method, 1));
		assertFalse("Expected no exclusion action for last value " + value + " in source: " + source, action.isEnabled()); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse("Expected direct exclusion of last value " + value + " to be refused in source: " + source, //$NON-NLS-1$ //$NON-NLS-2$
				EnumSourceValidator.excludeEnumValue(method, value));
		assertEquals("Expected refused exclusion to leave source unchanged", source, method.getCompilationUnit().getSource()); //$NON-NLS-1$
	}

	private void assertSuccessfulRun(IType type, int expectedInvocations) throws Exception {
		TestRunLog log= new TestRunLog();
		TestRunLog details= new TestRunLog();
		AtomicReference<Result> completed= new AtomicReference<>();
		TestRunListener listener= new TestRunListener() {
			@Override
			public void testCaseFinished(ITestCaseElement testCase) {
				log.add(testCase.getTestResult(false).toString());
				var failure= testCase.getFailureTrace();
				details.add(testCase.getTestClassName() + '#' + testCase.getTestMethodName()
						+ ": " + testCase.getTestResult(false) //$NON-NLS-1$
						+ (failure == null ? "" : System.lineSeparator() + failure.getTrace())); //$NON-NLS-1$
			}

			@Override
			public void sessionFinished(ITestRunSession session) {
				completed.set(session.getTestResult(true));
				log.setDone();
			}
		};
		JUnitCore.addTestRunListener(listener);
		try {
			String[] results= launchJUnit(type, TestKindRegistry.JUNIT5_TEST_KIND_ID, log);
			String context= sourceContext(type.getCompilationUnit()) + System.lineSeparator()
					+ "Observed invocations:" + System.lineSeparator() + String.join(System.lineSeparator(), details.getLog()) //$NON-NLS-1$
					+ System.lineSeparator() + "Result log: " + Arrays.toString(results); //$NON-NLS-1$
			assertNotNull("Expected the Jupiter session to finish for " + context, completed.get()); //$NON-NLS-1$
			assertEquals("Expected no parameterized-source runtime errors for " + context, Result.OK, completed.get()); //$NON-NLS-1$
			assertEquals("Expected exactly one invocation per remaining source in " + context, expectedInvocations, results.length); //$NON-NLS-1$
			for (int i= 0; i < results.length; i++) {
				assertEquals("Expected successful invocation at index " + i + " in " + context, Result.OK.toString(), results[i]); //$NON-NLS-1$ //$NON-NLS-2$
			}
		} finally {
			JUnitCore.removeTestRunListener(listener);
		}
	}
}
