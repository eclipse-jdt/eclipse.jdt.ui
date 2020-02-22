/*******************************************************************************
 * Copyright (c) 2007, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.junit.tests;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.junit.TestRunListener;
import org.eclipse.jdt.junit.model.ITestElement.FailureTrace;
import org.eclipse.jdt.junit.model.ITestElement.ProgressState;
import org.eclipse.jdt.junit.model.ITestElement.Result;
import org.eclipse.jdt.junit.tests.AbstractTestRunListenerTest.TestRunLog;
import org.eclipse.jdt.testplugin.JavaTestPlugin;

import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.internal.junit.model.JUnitModel;

import org.eclipse.jdt.ui.tests.core.rules.JUnit4WorkspaceTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.JUnitWorkspaceTestSetup;

public class TestRunSessionSerializationTests4 extends AbstractTestRunSessionSerializationTests {

	@Rule
	public JUnit4WorkspaceTestSetup jwts= new JUnit4WorkspaceTestSetup();

	@Test
	public void testATestCase() throws Exception {
		String test= "ATestCase";
		runCUTest(test);
	}

	@Test
	public void testATestSuite() throws Exception {
		String test= "ATestSuite";
		runCUTest(test);
	}

	@Test
	public void testFailures() throws Exception {
		String test= "Failures";

		TestRunLog log= new TestRunLog();
		final TestRunListener testRunListener= new TestRunListeners.SequenceTest(log);
		JUnitCore.addTestRunListener(testRunListener);
		try {
			runCUTest(test);
			String[] expectedSequence= new String[] {
					"sessionStarted-" + TestRunListeners.sessionAsString("Failures", ProgressState.RUNNING, Result.UNDEFINED, 0),

					"testCaseStarted-"  + TestRunListeners.testCaseAsString("testNasty", "pack.Failures", ProgressState.RUNNING, Result.UNDEFINED, null, 0),
					"testCaseFinished-" + TestRunListeners.testCaseAsString("testNasty", "pack.Failures", ProgressState.COMPLETED, Result.FAILURE, new FailureTrace("java.lang.AssertionError", null, null), 0),

					"testCaseStarted-"  + TestRunListeners.testCaseAsString("ignored", "pack.Failures", ProgressState.RUNNING, Result.UNDEFINED, null, 0),
					"testCaseFinished-" + TestRunListeners.testCaseAsString("ignored", "pack.Failures", ProgressState.COMPLETED, Result.IGNORED, null, 0),

					"testCaseStarted-"  + TestRunListeners.testCaseAsString("testError", "pack.Failures", ProgressState.RUNNING, Result.UNDEFINED, null, 0),
					"testCaseFinished-" + TestRunListeners.testCaseAsString("testError", "pack.Failures", ProgressState.COMPLETED, Result.ERROR, new FailureTrace("java.lang.IllegalStateException", null, null), 0),

					"testCaseStarted-"  + TestRunListeners.testCaseAsString("errorExpected", "pack.Failures", ProgressState.RUNNING, Result.UNDEFINED, null, 0),
					"testCaseFinished-" + TestRunListeners.testCaseAsString("errorExpected", "pack.Failures", ProgressState.COMPLETED, Result.OK, null, 0),

					"testCaseStarted-"  + TestRunListeners.testCaseAsString("errorExpectedOther", "pack.Failures", ProgressState.RUNNING, Result.UNDEFINED, null, 0),
					"testCaseFinished-" + TestRunListeners.testCaseAsString("errorExpectedOther", "pack.Failures", ProgressState.COMPLETED, Result.ERROR, new FailureTrace("java.lang.Exception", null, null), 0),

					"testCaseStarted-"  + TestRunListeners.testCaseAsString("compareTheStuff", "pack.Failures", ProgressState.RUNNING, Result.UNDEFINED, null, 0),
					"testCaseFinished-" + TestRunListeners.testCaseAsString("compareTheStuff", "pack.Failures", ProgressState.COMPLETED, Result.FAILURE, new FailureTrace("org.junit.ComparisonFailure", "\nHello World.\n\n", "\n\nHello my friend."), 0),

					"testCaseStarted-"  + TestRunListeners.testCaseAsString("testCompareNull", "pack.Failures", ProgressState.RUNNING, Result.UNDEFINED, null, 0),
					"testCaseFinished-" + TestRunListeners.testCaseAsString("testCompareNull", "pack.Failures", ProgressState.COMPLETED, Result.FAILURE, new FailureTrace("java.lang.AssertionError", null, null), 0),

					"sessionFinished-" + TestRunListeners.sessionAsString("Failures", ProgressState.COMPLETED, Result.ERROR, 0)
				};
			String[] actual= log.getLog();
			AbstractTestRunListenerTest.assertEqualLog(expectedSequence, actual);
		} finally {
			JUnitCore.removeTestRunListener(testRunListener);
		}
	}

	@Test
	public void testAllTests() throws Exception {
		String test= "AllTests";
		runCUTest(test);
	}

	@Test
	public void testImportAntSuite() throws Exception {
		Path testsPath= new Path(JUnitWorkspaceTestSetup.getProjectPath() + "ant/result/TESTS-TestSuites.xml");
		File testsFile= JavaTestPlugin.getDefault().getFileInPlugin(testsPath);
		JUnitModel.importTestRunSession(testsFile); // no contents check for now...
	}

}
