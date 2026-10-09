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
 *     Carsten Hammer - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.junit.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;

import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.junit.TestRunListener;
import org.eclipse.jdt.junit.model.ITestElement;
import org.eclipse.jdt.junit.model.ITestElement.ProgressState;
import org.eclipse.jdt.junit.model.ITestElement.Result;
import org.eclipse.jdt.junit.model.ITestRunSession;
import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.swt.widgets.Table;

import org.eclipse.jface.viewers.TableViewer;

import org.eclipse.ui.IWorkbenchPage;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.junit.JUnitCorePlugin;
import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;
import org.eclipse.jdt.internal.junit.model.JUnitModel;
import org.eclipse.jdt.internal.junit.model.TestCaseElement;
import org.eclipse.jdt.internal.junit.model.TestElement;
import org.eclipse.jdt.internal.junit.model.TestElement.Status;
import org.eclipse.jdt.internal.junit.model.TestRunSession;
import org.eclipse.jdt.internal.junit.model.TestSuiteElement;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.internal.junit.ui.TestRunnerViewPart;
import org.eclipse.jdt.internal.junit.ui.TestSessionLabelProvider;

public class DisabledParameterizedTestViewTest extends AbstractTestRunListenerTest {

	private static final String DISABLED_PARAMETERIZED_TEST=
			"""
			package pack;
			import org.junit.jupiter.api.Disabled;
			import org.junit.jupiter.params.ParameterizedTest;
			import org.junit.jupiter.params.provider.CsvSource;
			public class ATestCase {
			    @Disabled("Issue 53")
			    @ParameterizedTest
			    @CsvSource({ "input1, output1", "input2, output2" })
			    public void disabledParameterizedTest(String input, String expected) {
			    }
			}""";

	@Override
	@Before
	public void setUp() throws Exception {
		fProject= JavaProjectHelper.createJavaProject("DisabledParameterizedTestViewTest", "bin"); //$NON-NLS-1$ //$NON-NLS-2$
		JavaProjectHelper.addToClasspath(fProject, JavaCore.newContainerEntry(JUnitCore.JUNIT5_CONTAINER_PATH));
		JavaProjectHelper.addRTJar18(fProject);
	}

	@Test
	public void testViewAndIgnoredFilterAreUpdated() throws Exception {
		IWorkbenchPage activePage= JUnitPlugin.getActivePage();
		TestRunnerViewPart testRunnerViewPart= (TestRunnerViewPart) activePage.showView(TestRunnerViewPart.NAME);
		testRunnerViewPart.setLayoutMode(TestRunnerViewPart.LAYOUT_FLAT);

		ChangeRecordingListener changeListener= new ChangeRecordingListener();
		try {
			TestRunSession session= runTest(DISABLED_PARAMETERIZED_TEST, changeListener);
			assertEquals(List.of(ProgressState.RUNNING, ProgressState.COMPLETED), changeListener.fProgressStates);
			assertEquals(List.of(false, true), changeListener.fIgnoredStates);

			testRunnerViewPart.getTestViewer().processChangesInUI();
			Table table= ((TableViewer) testRunnerViewPart.getTestViewer().getActiveViewer()).getTable();
			assertEquals(1, table.getItemCount());
			assertTrue(table.getItem(0).getData() instanceof TestSuiteElement);
			TestSuiteElement ignoredSuite= (TestSuiteElement) table.getItem(0).getData();
			assertTrue(ignoredSuite.isIgnored());

			TestSessionLabelProvider labelProvider= new TestSessionLabelProvider(testRunnerViewPart, TestRunnerViewPart.LAYOUT_FLAT);
			try {
				TestSuiteElement parent= new TestSuiteElement(null, "parent", "parent", 1, null, null, null); //$NON-NLS-1$ //$NON-NLS-2$
				TestCaseElement ignoredTestCase= new TestCaseElement(parent, "ignored", "ignored(parent)", null, false, null, null); //$NON-NLS-1$ //$NON-NLS-2$
				ignoredTestCase.setIgnored(true);
				TestSuiteElement successfulSuite= new TestSuiteElement(null, "successful", "successful", 0, null, null, null); //$NON-NLS-1$ //$NON-NLS-2$
				successfulSuite.setStatus(Status.OK);
				assertSame(labelProvider.getImage(ignoredTestCase), table.getItem(0).getImage());
				assertNotSame(labelProvider.getImage(successfulSuite), table.getItem(0).getImage());
			} finally {
				labelProvider.dispose();
			}

			testRunnerViewPart.getTestViewer().setShowFailuresOrIgnoredOnly(false, true, TestRunnerViewPart.LAYOUT_FLAT);
			assertEquals(1, table.getItemCount());
			assertSame(ignoredSuite, table.getItem(0).getData());
			assertSession(session, 1, 1);
		} finally {
			testRunnerViewPart.getTestViewer().setShowFailuresOrIgnoredOnly(false, false, TestRunnerViewPart.LAYOUT_HIERARCHICAL);
		}
	}

	@Test
	public void testMixedEnabledAndDisabledTestsAreCounted() throws Exception {
		String source=
				"""
				package pack;
				import org.junit.jupiter.api.Disabled;
				import org.junit.jupiter.api.Test;
				import org.junit.jupiter.params.ParameterizedTest;
				import org.junit.jupiter.params.provider.ValueSource;
				public class ATestCase {
				    @Test
				    public void enabledTest() {
				    }
				    @Disabled("Issue 53")
				    @ParameterizedTest
				    @ValueSource(strings = { "one", "two" })
				    public void disabledParameterizedTest(String value) {
				    }
				}""";

		assertSession(runTest(source, new ChangeRecordingListener()), 2, 1);
	}

	@Test
	public void testMultipleDisabledParameterizedTestsAreCountedOnceEach() throws Exception {
		String source=
				"""
				package pack;
				import org.junit.jupiter.api.Disabled;
				import org.junit.jupiter.params.ParameterizedTest;
				import org.junit.jupiter.params.provider.ValueSource;
				public class ATestCase {
				    @Disabled("Issue 53")
				    @ParameterizedTest
				    @ValueSource(strings = { "one", "two" })
				    public void first(String value) {
				    }
				    @Disabled("Issue 53")
				    @ParameterizedTest
				    @ValueSource(ints = { 1, 2 })
				    public void second(int value) {
				    }
				}""";

		assertSession(runTest(source, new ChangeRecordingListener()), 2, 2);
	}

	@Test
	public void testDisabledClassCountsNormalAndParameterizedTests() throws Exception {
		String source=
				"""
				package pack;
				import org.junit.jupiter.api.Disabled;
				import org.junit.jupiter.api.Test;
				import org.junit.jupiter.params.ParameterizedTest;
				import org.junit.jupiter.params.provider.ValueSource;
				@Disabled("Issue 53")
				public class ATestCase {
				    @Test
				    public void normalTest() {
				    }
				    @ParameterizedTest
				    @ValueSource(strings = { "one", "two" })
				    public void parameterizedTest(String value) {
				    }
				}""";

		assertSession(runTest(source, new ChangeRecordingListener()), 2, 2);
	}

	@Test
	public void testIgnoredSuiteDoesNotHideFailureAfterRoundTrip() throws Exception {
		TestRunSession session= new TestRunSession("ignored failure", fProject); //$NON-NLS-1$
		TestSuiteElement ignoredFailure= (TestSuiteElement) session.createTestElement(session.getTestRoot(),
				"1", "ignoredFailure", true, 0, false, null, null, null); //$NON-NLS-1$ //$NON-NLS-2$
		ignoredFailure.setIgnored(true);
		session.registerTestFailureStatus(ignoredFailure, Status.FAILURE, "failure", null, null); //$NON-NLS-1$
		session.registerTestEnded(ignoredFailure, true);

		assertEquals(Result.FAILURE, ignoredFailure.getTestResult(false));
		assertEquals(Result.FAILURE, ignoredFailure.getTestResult(true));

		IWorkbenchPage activePage= JUnitPlugin.getActivePage();
		TestRunnerViewPart testRunnerViewPart= (TestRunnerViewPart) activePage.showView(TestRunnerViewPart.NAME);
		TestSuiteElement regularFailure= new TestSuiteElement(null, "2", "regularFailure", 0, null, null, null); //$NON-NLS-1$ //$NON-NLS-2$
		regularFailure.setStatus(Status.FAILURE);
		TestSessionLabelProvider labelProvider= new TestSessionLabelProvider(testRunnerViewPart, TestRunnerViewPart.LAYOUT_HIERARCHICAL);
		try {
			assertSame(labelProvider.getImage(regularFailure), labelProvider.getImage(ignoredFailure));
		} finally {
			labelProvider.dispose();
		}

		File file= File.createTempFile("ignored-suite-failure", ".xml"); //$NON-NLS-1$ //$NON-NLS-2$
		TestRunSession importedSession= null;
		try {
			JUnitModel.exportTestRunSession(session, file);
			importedSession= JUnitModel.importTestRunSession(file);
			TestSuiteElement importedFailure= (TestSuiteElement) importedSession.getChildren()[0];
			assertTrue(importedFailure.isIgnored());
			assertEquals(Result.FAILURE, importedFailure.getTestResult(false));
			assertEquals(Result.FAILURE, importedFailure.getTestResult(true));
			assertEquals(1, importedSession.getFailureCount());
		} finally {
			if (importedSession != null) {
				JUnitCorePlugin.getModel().removeTestRunSession(importedSession);
			}
			Files.deleteIfExists(file.toPath());
		}
	}

	private TestRunSession runTest(String source, ChangeRecordingListener changeListener) throws Exception {
		IType testType= createType(source, "pack", "ATestCase.java"); //$NON-NLS-1$ //$NON-NLS-2$
		TestRunLog log= new TestRunLog();
		AtomicReference<TestRunSession> finishedSession= new AtomicReference<>();
		TestRunListener testRunListener= new TestRunListener() {
			@Override
			public void sessionStarted(ITestRunSession session) {
				((TestRunSession) session).addTestSessionListener(changeListener);
			}

			@Override
			public void sessionFinished(ITestRunSession session) {
				finishedSession.set((TestRunSession) session);
				log.setDone();
			}
		};
		JUnitCore.addTestRunListener(testRunListener);
		try {
			launchJUnit(testType, TestKindRegistry.JUNIT5_TEST_KIND_ID, log);
		} finally {
			JUnitCore.removeTestRunListener(testRunListener);
		}

		TestRunSession session= finishedSession.get();
		assertNotNull(session);
		session.removeTestSessionListener(changeListener);
		return session;
	}

	private static void assertSession(TestRunSession session, int total, int ignored) {
		assertEquals(total, session.getTotalCount());
		assertEquals(total, session.getStartedCount());
		assertEquals(ignored, session.getIgnoredCount());
		assertEquals(0, session.getAssumptionFailureCount());
		assertEquals(0, session.getFailureCount());
		assertEquals(0, session.getErrorCount());
		assertEquals(Result.OK, session.getTestResult(true));
		assertEquals(ignored, countIgnoredElements(session.getChildren()));
	}

	private static int countIgnoredElements(ITestElement[] elements) {
		int ignored= 0;
		for (ITestElement element : elements) {
			if (element.getTestResult(false) == Result.IGNORED) {
				ignored++;
			}
			if (element instanceof TestSuiteElement) {
				ignored+= countIgnoredElements(((TestSuiteElement) element).getChildren());
			}
		}
		return ignored;
	}

	private static class ChangeRecordingListener extends TestRunListenerTest5.TestSessionListener {
		final List<ProgressState> fProgressStates= new CopyOnWriteArrayList<>();
		final List<Boolean> fIgnoredStates= new CopyOnWriteArrayList<>();

		@Override
		public void testChanged(TestElement testElement) {
			if (testElement instanceof TestSuiteElement) {
				TestSuiteElement testSuiteElement= (TestSuiteElement) testElement;
				fProgressStates.add(testElement.getProgressState());
				fIgnoredStates.add(testSuiteElement.isIgnored());
			}
		}

		@Override
		public boolean acceptsSwapToDisk() {
			return true;
		}
	}
}
