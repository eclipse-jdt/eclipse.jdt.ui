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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
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

import org.eclipse.core.runtime.IPath;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.junit.JUnitCorePlugin;
import org.eclipse.jdt.internal.junit.model.JUnitModel;
import org.eclipse.jdt.internal.junit.model.TestRunSession;
import org.eclipse.jdt.internal.junit.model.TestSuiteElement;

public abstract class AbstractDisabledParameterizedTestRunSupport extends AbstractTestRunListenerTest {

	@Override
	@Before
	public void setUp() throws Exception {
		fProject= JavaProjectHelper.createJavaProject(getClass().getSimpleName(), "bin"); //$NON-NLS-1$
		JavaProjectHelper.addToClasspath(fProject, JavaCore.newContainerEntry(getJUnitContainerPath()));
		addRuntimeLibrary();
	}

	protected abstract IPath getJUnitContainerPath();

	protected abstract String getTestKindId();

	protected abstract void addRuntimeLibrary() throws Exception;

	@Test
	public void testDisabledParameterizedTest() throws Exception {
		String source=
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
		IType testType= createType(source, "pack", "ATestCase.java"); //$NON-NLS-1$ //$NON-NLS-2$

		TestRunLog log= new TestRunLog();
		AtomicReference<TestRunSession> finishedSession= new AtomicReference<>();
		TestRunListener testRunListener= new TestRunListener() {
			@Override
			public void sessionFinished(ITestRunSession session) {
				finishedSession.set((TestRunSession) session);
				log.setDone();
			}
		};
		JUnitCore.addTestRunListener(testRunListener);
		try {
			launchJUnit(testType, getTestKindId(), log);
		} finally {
			JUnitCore.removeTestRunListener(testRunListener);
		}

		TestRunSession session= finishedSession.get();
		assertNotNull(session);
		assertDisabledParameterizedTest(session);

		File file= File.createTempFile("disabled-parameterized-test", ".xml"); //$NON-NLS-1$ //$NON-NLS-2$
		TestRunSession importedSession= null;
		try {
			JUnitModel.exportTestRunSession(session, file);
			importedSession= JUnitModel.importTestRunSession(file);
			assertDisabledParameterizedTest(importedSession);
		} finally {
			if (importedSession != null) {
				JUnitCorePlugin.getModel().removeTestRunSession(importedSession);
			}
			Files.deleteIfExists(file.toPath());
		}
	}

	private static void assertDisabledParameterizedTest(TestRunSession session) {
		assertEquals(1, session.getTotalCount());
		assertEquals(1, session.getStartedCount());
		assertEquals(1, session.getIgnoredCount());
		assertEquals(0, session.getAssumptionFailureCount());
		assertEquals(0, session.getFailureCount());
		assertEquals(0, session.getErrorCount());
		assertEquals(Result.OK, session.getTestResult(true));

		TestSuiteElement ignoredSuite= findIgnoredSuite(session.getChildren());
		assertNotNull(ignoredSuite);
		assertTrue(ignoredSuite.getTestName().startsWith("disabledParameterizedTest(")); //$NON-NLS-1$
		assertEquals(0, ignoredSuite.getChildren().length);
		assertEquals(ProgressState.COMPLETED, ignoredSuite.getProgressState());
		assertEquals(Result.IGNORED, ignoredSuite.getTestResult(false));
	}

	private static TestSuiteElement findIgnoredSuite(ITestElement[] elements) {
		for (ITestElement element : elements) {
			if (element instanceof TestSuiteElement) {
				TestSuiteElement testSuiteElement= (TestSuiteElement) element;
				if (testSuiteElement.isIgnored()) {
					return testSuiteElement;
				}
				TestSuiteElement ignoredSuite= findIgnoredSuite(testSuiteElement.getChildren());
				if (ignoredSuite != null) {
					return ignoredSuite;
				}
			}
		}
		return null;
	}
}
