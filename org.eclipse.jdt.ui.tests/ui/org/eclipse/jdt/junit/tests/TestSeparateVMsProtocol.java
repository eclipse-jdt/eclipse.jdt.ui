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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Test;

import org.eclipse.jdt.launching.SocketUtil;

import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;
import org.eclipse.jdt.internal.junit.model.ITestRunListener2;
import org.eclipse.jdt.internal.junit.model.RemoteTestRunnerClient;
import org.eclipse.jdt.internal.junit.runner.MessageIds;

/**
 * Tests the client that listens to the test runs of several VMs, which connect to it one after the
 * other.
 */
public class TestSeparateVMsProtocol {

	private static final long TIMEOUT= 10_000;

	private static class RecordingListener implements ITestRunListener2 {
		final BlockingQueue<String> events= new LinkedBlockingQueue<>();

		@Override
		public void testRunStarted(int testCount) {
			events.add("runStarted:" + testCount);
		}

		@Override
		public void testRunEnded(long elapsedTime) {
			events.add("runEnded:" + elapsedTime);
		}

		@Override
		public void testRunStopped(long elapsedTime) {
			events.add("runStopped:" + elapsedTime);
		}

		@Override
		public void testRunTerminated() {
			events.add("runTerminated");
		}

		@Override
		public void testRunInterrupted() {
			events.add("runInterrupted");
		}

		@Override
		public void testStarted(String testId, String testName) {
		}

		@Override
		public void testEnded(String testId, String testName) {
		}

		@Override
		public void testFailed(int status, String testId, String testName, String trace, String expected, String actual) {
			events.add("failed:" + expected);
		}

		@Override
		public void testTreeEntry(String description) {
			events.add("tree:" + description);
		}

		@Override
		public void testReran(String testId, String testClass, String testName, int status, String trace, String expected, String actual) {
		}
	}

	/**
	 * A VM that performs a test run: it connects to the client and sends it messages.
	 */
	private class TestVM implements AutoCloseable {
		private final Socket fSocket;

		private final PrintWriter fWriter;

		private final BufferedReader fReader;

		TestVM() throws Exception {
			fSocket= connect();
			// a message that does not come fails the test instead of blocking it
			fSocket.setSoTimeout((int) TIMEOUT);
			fWriter= new PrintWriter(new OutputStreamWriter(fSocket.getOutputStream(), StandardCharsets.UTF_8), true);
			fReader= new BufferedReader(new InputStreamReader(fSocket.getInputStream(), StandardCharsets.UTF_8));
		}

		private Socket connect() throws Exception {
			long end= System.currentTimeMillis() + TIMEOUT;
			while (true) {
				try {
					return new Socket("localhost", fPort);
				} catch (IOException e) {
					// the client has not opened its server socket yet
					if (System.currentTimeMillis() > end) {
						throw e;
					}
					Thread.sleep(20);
				}
			}
		}

		void send(String... messages) {
			for (String message : messages) {
				fWriter.println(message);
			}
		}

		String receive() throws IOException {
			return fReader.readLine();
		}

		@Override
		public void close() throws IOException {
			fSocket.close();
		}
	}

	private final RecordingListener fListener= new RecordingListener();

	private RemoteTestRunnerClient fClient;

	private int fPort;

	private void startListening(String testKindId, int testRunCount) {
		fPort= SocketUtil.findFreePort();
		fClient= new RemoteTestRunnerClient(TestKindRegistry.getDefault().getKind(testKindId));
		fClient.startListening(new ITestRunListener2[] { fListener }, fPort, testRunCount);
	}

	/**
	 * Waits until the client has read the end of the connection of the VM: it has handled all the
	 * messages of the VM.
	 */
	private void awaitConnectionClosed() throws InterruptedException {
		long end= System.currentTimeMillis() + TIMEOUT;
		synchronized (fClient) {
			while (fClient.isRunning()) {
				long remaining= end - System.currentTimeMillis();
				assertTrue("the connection has not been closed", remaining > 0);
				fClient.wait(Math.min(remaining, 20));
			}
		}
	}

	private void assertEvent(String expected) throws InterruptedException {
		assertEquals(expected, fListener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS));
	}

	@After
	public void tearDown() {
		// closes the server socket if a VM is still expected
		fClient.stopWaiting();
	}

	@Test
	public void testElapsedTimeOfEndedTestRuns() throws Exception {
		startListening(TestKindRegistry.JUNIT4_TEST_KIND_ID, 2);
		try (TestVM vm= new TestVM()) {
			vm.send(MessageIds.TEST_RUN_START + "1 v2", MessageIds.TEST_RUN_END + "100");
		}
		assertEvent("runStarted:1");
		try (TestVM vm= new TestVM()) {
			vm.send(MessageIds.TEST_RUN_START + "2 v2", MessageIds.TEST_RUN_END + "50");
		}
		assertEvent("runStarted:2");
		assertEvent("runEnded:150");
	}

	/**
	 * The VM of a stopped test run tells its elapsed time, to which the one of the VMs that ran
	 * before it is added.
	 */
	@Test
	public void testElapsedTimeOfStoppedTestRun() throws Exception {
		startListening(TestKindRegistry.JUNIT4_TEST_KIND_ID, 3);
		try (TestVM vm= new TestVM()) {
			vm.send(MessageIds.TEST_RUN_START + "1 v2", MessageIds.TEST_RUN_END + "100");
		}
		assertEvent("runStarted:1");
		try (TestVM vm= new TestVM()) {
			vm.send(MessageIds.TEST_RUN_START + "2 v2");
			assertEvent("runStarted:2");

			fClient.stopTest();
			assertEquals(MessageIds.TEST_STOP, vm.receive());
			vm.send(MessageIds.TEST_STOPPED + "50");
			assertEvent("runStopped:150");
		}
	}

	/**
	 * The VM of a stopped JUnit 5 test run does not tell its elapsed time: the one of the VMs that
	 * ran before it is still reported.
	 */
	@Test
	public void testElapsedTimeOfStoppedJUnit5TestRun() throws Exception {
		startListening(TestKindRegistry.JUNIT5_TEST_KIND_ID, 3);
		try (TestVM vm= new TestVM()) {
			vm.send(MessageIds.TEST_RUN_START + "1 v2", MessageIds.TEST_RUN_END + "100");
		}
		assertEvent("runStarted:1");
		try (TestVM vm= new TestVM()) {
			vm.send(MessageIds.TEST_RUN_START + "2 v2");
			assertEvent("runStarted:2");

			fClient.stopTest();
			assertEquals(MessageIds.TEST_STOP, vm.receive());
			// see JUnit5TestReference
			vm.send(MessageIds.TEST_ERROR + "1,test(pack.ATest)",
					MessageIds.TRACE_START,
					"java.lang.OutOfMemoryError: Junit5 test stopped by user",
					MessageIds.TRACE_END);
			assertEvent("runStopped:100");
		}
	}

	/**
	 * The VM of a test run that has ended sends nothing more, not even TEST_STOPPED: the test runs
	 * stopped after it are reported stopped when its connection closes.
	 */
	@Test
	public void testStopAfterTheEndOfATestRunFollowedByOthers() throws Exception {
		startListening(TestKindRegistry.JUNIT4_TEST_KIND_ID, 3);
		try (TestVM vm= new TestVM()) {
			// the tree entry tells that the end of the test run has been read
			vm.send(MessageIds.TEST_RUN_START + "1 v2", MessageIds.TEST_RUN_END + "100", MessageIds.TEST_TREE + "1,test(pack.ATest),false,1");
			assertEvent("runStarted:1");
			assertEvent("tree:1,test(pack.ATest),false,1");

			fClient.stopTest();
			assertEquals(MessageIds.TEST_STOP, vm.receive());
		}
		assertEvent("runStopped:100");
	}

	/**
	 * When the test runs end normally, all the VMs may have exited while the one of a previous test
	 * run is still read: the next one, which has connected, is read too.
	 */
	@Test
	public void testVMsThatHaveConnectedAreReadAfterTheLaunchEnded() throws Exception {
		startListening(TestKindRegistry.JUNIT4_TEST_KIND_ID, 2);
		try (TestVM vm1= new TestVM()) {
			vm1.send(MessageIds.TEST_RUN_START + "1 v2");
			assertEvent("runStarted:1");
			try (TestVM vm2= new TestVM()) {
				vm2.send(MessageIds.TEST_RUN_START + "2 v2", MessageIds.TEST_RUN_END + "50");
			}
			// both VMs have exited
			fClient.stopWaiting();
			vm1.send(MessageIds.TEST_RUN_END + "100");
		}
		assertEvent("runStarted:2");
		assertEvent("runEnded:150");
	}

	/**
	 * A VM that exits without connecting, e.g. because it cannot start, ends the test runs.
	 */
	@Test
	public void testVMThatDoesNotConnect() throws Exception {
		startListening(TestKindRegistry.JUNIT4_TEST_KIND_ID, 2);
		try (TestVM vm= new TestVM()) {
			vm.send(MessageIds.TEST_RUN_START + "1 v2", MessageIds.TEST_RUN_END + "100");
		}
		assertEvent("runStarted:1");
		fClient.stopWaiting();
		assertEvent("runTerminated");
	}

	/**
	 * A VM started before the test runs were stopped while no VM was connected is stopped as soon as
	 * it connects.
	 */
	@Test
	public void testStopWhileNoVMIsConnected() throws Exception {
		startListening(TestKindRegistry.JUNIT4_TEST_KIND_ID, 3);
		try (TestVM vm= new TestVM()) {
			vm.send(MessageIds.TEST_RUN_START + "1 v2", MessageIds.TEST_RUN_END + "100");
		}
		assertEvent("runStarted:1");
		awaitConnectionClosed();
		fClient.stopTest();
		try (TestVM vm= new TestVM()) {
			assertEquals(MessageIds.TEST_STOP, vm.receive());
			vm.send(MessageIds.TEST_RUN_START + "2 v2", MessageIds.TEST_STOPPED + "50");
			assertEvent("runStarted:2");
			assertEvent("runStopped:150");
		}
	}

	/**
	 * Test runs stopped while no VM was connected, whose next VMs have been canceled, are stopped,
	 * not terminated.
	 */
	@Test
	public void testStopWhileNoVMIsConnectedAndNoOtherVMConnects() throws Exception {
		startListening(TestKindRegistry.JUNIT4_TEST_KIND_ID, 3);
		try (TestVM vm= new TestVM()) {
			vm.send(MessageIds.TEST_RUN_START + "1 v2", MessageIds.TEST_RUN_END + "100");
		}
		assertEvent("runStarted:1");
		awaitConnectionClosed();
		fClient.stopTest();
		fClient.stopWaiting();
		assertEvent("runStopped:100");
		assertNoEvent();
	}

	/**
	 * A VM that exits without ending its test run, e.g. in a fixture method, is reported to the
	 * listeners before the next VM starts, also if it exited before its first test.
	 */
	@Test
	public void testVMThatExitsWithoutEndingItsTestRun() throws Exception {
		startListening(TestKindRegistry.JUNIT4_TEST_KIND_ID, 3);
		try (TestVM vm= new TestVM()) {
			vm.send(MessageIds.TEST_RUN_START + "1 v2");
		}
		assertEvent("runStarted:1");
		assertEvent("runInterrupted");
		try (TestVM vm= new TestVM()) {
			vm.send(MessageIds.TEST_RUN_START + "1 v2", MessageIds.TEST_RUN_END + "50");
		}
		assertEvent("runStarted:1");
		assertNoEvent();
	}

	/**
	 * The comparison values of a failure that a VM did not finish to report are not used for the
	 * failures of the next VM.
	 */
	@Test
	public void testVMThatExitsWhileReportingAComparisonFailure() throws Exception {
		startListening(TestKindRegistry.JUNIT4_TEST_KIND_ID, 2);
		try (TestVM vm= new TestVM()) {
			vm.send(MessageIds.TEST_RUN_START + "1 v2", MessageIds.EXPECTED_START, "expected");
		}
		assertEvent("runStarted:1");
		assertEvent("runInterrupted");
		try (TestVM vm= new TestVM()) {
			vm.send(MessageIds.TEST_RUN_START + "1 v2", MessageIds.TEST_TREE + "1,test(pack.ATest),false,1",
					MessageIds.TEST_FAILED + "1,test(pack.ATest)", MessageIds.TRACE_START, "trace", MessageIds.TRACE_END,
					MessageIds.TEST_RUN_END + "50");
		}
		assertEvent("runStarted:1");
		assertEvent("tree:1,test(pack.ATest),false,1");
		assertEvent("failed:null");
	}

	/**
	 * A JUnit 5 VM that does not connect ends the test runs once.
	 */
	@Test
	public void testJUnit5VMThatDoesNotConnect() throws Exception {
		startListening(TestKindRegistry.JUNIT5_TEST_KIND_ID, 2);
		try (TestVM vm= new TestVM()) {
			vm.send(MessageIds.TEST_RUN_START + "1 v2", MessageIds.TEST_RUN_END + "100");
		}
		assertEvent("runStarted:1");
		fClient.stopWaiting();
		assertEvent("runTerminated");
		assertNoEvent();
	}

	private void assertNoEvent() throws InterruptedException {
		assertEquals(null, fListener.events.poll(1, TimeUnit.SECONDS));
	}
}
