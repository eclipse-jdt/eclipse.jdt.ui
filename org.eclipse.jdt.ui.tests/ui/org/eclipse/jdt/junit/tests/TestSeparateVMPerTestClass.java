/*******************************************************************************
 * Copyright (c) 2026 Hélios GILLES and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Hélios GILLES - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.junit.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.junit.TestRunListener;
import org.eclipse.jdt.junit.model.ITestCaseElement;
import org.eclipse.jdt.junit.model.ITestElement.ProgressState;
import org.eclipse.jdt.junit.model.ITestElement.Result;
import org.eclipse.jdt.junit.model.ITestRunSession;
import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants;
import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;

/**
 * Tests running each test class of a package in a separate VM.
 */
public class TestSeparateVMPerTestClass extends AbstractTestRunListenerTest {

	/**
	 * A test class whose test only passes if no other test class has been run in its VM.
	 */
	private static final String TEST_CLASS= """
			package %1$s;
			public class %2$s {
				@%3$s public static void countTestClass() { pack.Shared.testClasses++; }
				@%4$s public void testSeparateVM() { %5$s.assertEquals(1, pack.Shared.testClasses); }
				%6$s
			}""";

	@Override
	@Before
	public void setUp() throws Exception {
		fProject= JavaProjectHelper.createJavaProject("TestSeparateVMPerTestClass", "bin");
		JavaProjectHelper.addRTJar18(fProject);
	}

	@Test
	public void testJUnit4() throws Exception {
		JavaProjectHelper.addToClasspath(fProject, JavaCore.newContainerEntry(JUnitCore.JUNIT4_CONTAINER_PATH));
		IPackageFragment pack= createTestClasses("org.junit.BeforeClass", "org.junit.Test", "org.junit.Assert");

		runInSeparateVMs(pack, TestKindRegistry.JUNIT4_TEST_KIND_ID, 2, List.of(
				"pack.ATest.testSeparateVM=OK",
				"pack.BTest.testFail=Failure",
				"pack.BTest.testSeparateVM=OK"));
	}

	@Test
	public void testJUnit5WithSubpackage() throws Exception {
		JavaProjectHelper.addToClasspath(fProject, JavaCore.newContainerEntry(JUnitCore.JUNIT5_CONTAINER_PATH));
		IPackageFragment pack= createTestClasses("org.junit.jupiter.api.BeforeAll", "org.junit.jupiter.api.Test", "org.junit.jupiter.api.Assertions");
		createType(TEST_CLASS.formatted("pack.sub", "CTest", "org.junit.jupiter.api.BeforeAll", "org.junit.jupiter.api.Test", "org.junit.jupiter.api.Assertions", ""), "pack.sub", "CTest.java");

		runInSeparateVMs(pack, TestKindRegistry.JUNIT5_TEST_KIND_ID, 3, List.of(
				"pack.ATest.testSeparateVM=OK",
				"pack.BTest.testFail=Failure",
				"pack.BTest.testSeparateVM=OK",
				"pack.sub.CTest.testSeparateVM=OK"));
	}

	private IPackageFragment createTestClasses(String beforeClass, String test, String assertions) throws Exception {
		createType("package pack; public class Shared { public static int testClasses; }", "pack", "Shared.java");
		createType(TEST_CLASS.formatted("pack", "ATest", beforeClass, test, assertions, ""), "pack", "ATest.java");
		String testFail= "@" + test + " public void testFail() { " + assertions + ".fail(); }";
		return createType(TEST_CLASS.formatted("pack", "BTest", beforeClass, test, assertions, testFail), "pack", "BTest.java").getPackageFragment();
	}

	private void runInSeparateVMs(IPackageFragment pack, String testKindId, int expectedVMs, List<String> expectedResults) throws Exception {
		TestRunLog log= new TestRunLog();
		TestRunListener testRunListener= new TestRunListener() {
			@Override
			public void sessionStarted(ITestRunSession session) {
				log.add("sessionStarted");
			}

			@Override
			public void testCaseFinished(ITestCaseElement testCaseElement) {
				log.add(testCaseElement.getTestClassName() + "." + testCaseElement.getTestMethodName() + "=" + testCaseElement.getTestResult(false));
			}

			@Override
			public void sessionFinished(ITestRunSession session) {
				log.add("sessionFinished=" + session.getProgressState() + "," + session.getTestResult(true));
				log.setDone();
			}
		};
		JUnitCore.addTestRunListener(testRunListener);
		buildTestCase(pack);
		LaunchesListener launchesListener= new LaunchesListener();
		ILaunchConfigurationWorkingCopy configuration= createLaunchConfiguration(pack, testKindId, null, launchesListener);
		configuration.setAttribute(JUnitLaunchConfigurationConstants.ATTR_SEPARATE_VM_PER_TEST_CLASS, true);
		try {
			ILaunch launch= configuration.launch(ILaunchManager.RUN_MODE, null);
			assertTrue("Launch has not terminated", waitForCondition(launchesListener.fLaunchHasTerminated::get, 60 * 1000, 100));
			assertTrue("Session has not finished", waitForCondition(log::isDone, 15 * 1000, 100));
			assertEquals("VMs", expectedVMs, launch.getProcesses().length);
		} finally {
			cleanUp(configuration, launchesListener);
			JUnitCore.removeTestRunListener(testRunListener);
		}

		List<String> actual= List.of(log.getLog());
		assertEquals("sessionStarted", actual.get(0));
		assertEquals("sessionFinished=" + ProgressState.COMPLETED + "," + Result.FAILURE, actual.get(actual.size() - 1));
		assertEquals(expectedResults, actual.subList(1, actual.size() - 1).stream().sorted().toList());
	}
}
