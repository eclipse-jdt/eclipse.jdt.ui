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
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Proxy;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.junit.model.ITestElement;
import org.eclipse.jdt.junit.model.ITestElement.ProgressState;
import org.eclipse.jdt.junit.model.ITestElement.Result;
import org.eclipse.jdt.launching.SocketUtil;

import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants;
import org.eclipse.jdt.internal.junit.model.ITestSessionListener;
import org.eclipse.jdt.internal.junit.model.TestCaseElement;
import org.eclipse.jdt.internal.junit.model.TestElement;
import org.eclipse.jdt.internal.junit.model.TestElement.Status;
import org.eclipse.jdt.internal.junit.model.TestRunSession;
import org.eclipse.jdt.internal.junit.runner.MessageIds;

/**
 * Tests the session of test runs performed by several VMs, one after the other: the VMs are
 * simulated, they connect to the session and send it messages.
 */
public class TestSeparateVMsSession {

	private static final long TIMEOUT= 10_000;

	private static final long SECOND= 1_000_000_000L;

	private int fPort;

	private TestRunSession fSession;

	@Before
	public void setUp() {
		fPort= SocketUtil.findFreePort();
		Launch launch= new Launch(null, ILaunchManager.RUN_MODE, null);
		launch.setAttribute(JUnitLaunchConfigurationConstants.ATTR_VM_COUNT, "2"); //$NON-NLS-1$
		fSession= new TestRunSession(launch, JavaCore.create(ResourcesPlugin.getWorkspace().getRoot().getProject("TestProject")), fPort); //$NON-NLS-1$
		// the session stays in memory, as when the JUnit view shows it: else it may be swapped to
		// disk once ended, and read again without some of its times
		fSession.addTestSessionListener((ITestSessionListener) Proxy.newProxyInstance(getClass().getClassLoader(),
				new Class<?>[] { ITestSessionListener.class }, (proxy, method, args) -> switch (method.getName()) {
					case "acceptsSwapToDisk" -> Boolean.FALSE; //$NON-NLS-1$
					case "equals" -> proxy == args[0]; //$NON-NLS-1$
					case "hashCode" -> System.identityHashCode(proxy); //$NON-NLS-1$
					default -> null;
				}));
	}

	@After
	public void tearDown() {
		if (fSession.isRunning()) {
			fSession.stopTestRun();
		}
	}

	/**
	 * Runs a VM that connects to the session and sends it the given messages.
	 */
	private void runVM(String... messages) throws Exception {
		try (Socket socket= connect(); PrintWriter writer= new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {
			for (String message : messages) {
				writer.println(message);
			}
		}
	}

	private Socket connect() throws Exception {
		long end= System.currentTimeMillis() + TIMEOUT;
		while (true) {
			try {
				return new Socket("localhost", fPort); //$NON-NLS-1$
			} catch (IOException e) {
				// the session has not opened its server socket yet
				if (System.currentTimeMillis() > end) {
					throw e;
				}
				Thread.sleep(20);
			}
		}
	}

	/**
	 * The messages of a VM that runs one test.
	 *
	 * @param end whether the VM ends the test, else it exits during the test
	 */
	private static String[] oneTest(String className, long startNanos, long elapsedNanos, boolean end) {
		String test= "1,test(pack." + className + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		if (!end) {
			return new String[] { MessageIds.TEST_RUN_START + "1 v2", MessageIds.TEST_TREE + test + ",false,1", MessageIds.TEST_START + test }; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return new String[] { MessageIds.TEST_RUN_START + "1 v2", MessageIds.TEST_TREE + test + ",false,1", MessageIds.TEST_START + test, //$NON-NLS-1$ //$NON-NLS-2$
				MessageIds.TEST_TIMING + "1," + startNanos + "," + elapsedNanos + ",-1,-1", MessageIds.TEST_END + test, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				MessageIds.TEST_RUN_END + elapsedNanos / 1_000_000 };
	}

	private void awaitEnd() throws InterruptedException {
		long end= System.currentTimeMillis() + TIMEOUT;
		// the session reads the VMs in a thread of its own: it has started once it has read the first one
		while (fSession.getStartTime() == 0 || fSession.isRunning()) {
			if (System.currentTimeMillis() > end) {
				throw new AssertionError("the session has not ended"); //$NON-NLS-1$
			}
			Thread.sleep(20);
		}
	}

	/**
	 * @return the test of the given class: an ended session may be swapped to disk, then read again
	 *         with other IDs
	 */
	private TestElement test(String className) {
		for (ITestElement child : fSession.getTestRoot().getChildren()) {
			if (child instanceof TestCaseElement testCase && ("pack." + className).equals(testCase.getTestClassName())) { //$NON-NLS-1$
				return testCase;
			}
		}
		throw new AssertionError("no test of " + className); //$NON-NLS-1$
	}

	/**
	 * The times sent by a VM are relative to its start: the ones of the next VM follow them.
	 */
	@Test
	public void testElapsedTimeOfTheTestsOfSeveralVMs() throws Exception {
		runVM(oneTest("ATest", 0, 2 * SECOND, true)); //$NON-NLS-1$
		runVM(oneTest("BTest", 0, 2 * SECOND, true)); //$NON-NLS-1$
		awaitEnd();

		assertEquals(ProgressState.COMPLETED, fSession.getProgressState());
		assertEquals(Result.OK, fSession.getTestResult(true));
		assertEquals(4.0, fSession.getElapsedTimeInSeconds(), 0.001);
		assertEquals(2.0, test("BTest").getElapsedTimeInSeconds(), 0.001); //$NON-NLS-1$
	}

	/**
	 * A VM that exits during a test, e.g. with System.exit(), does not end it: it is an error, and
	 * the next VMs run.
	 */
	@Test
	public void testVMThatExitsDuringATest() throws Exception {
		runVM(oneTest("ExitingTest", 0, 0, false)); //$NON-NLS-1$
		runVM(oneTest("BTest", 0, SECOND, true)); //$NON-NLS-1$
		awaitEnd();

		assertEquals(Status.ERROR, test("ExitingTest").getStatus()); //$NON-NLS-1$
		assertFalse(test("ExitingTest").getStatus().isRunning()); //$NON-NLS-1$
		assertEquals(Status.OK, test("BTest").getStatus()); //$NON-NLS-1$
		assertEquals(ProgressState.COMPLETED, fSession.getProgressState());
		assertEquals(Result.ERROR, fSession.getTestResult(true));
		assertEquals(1, fSession.getErrorCount());
	}
}
