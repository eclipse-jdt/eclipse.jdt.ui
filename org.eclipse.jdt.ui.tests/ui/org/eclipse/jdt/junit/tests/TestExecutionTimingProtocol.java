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
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;
import org.eclipse.jdt.internal.junit.model.ITestRunListener2;
import org.eclipse.jdt.internal.junit.model.RemoteTestRunnerClient;
import org.eclipse.jdt.internal.junit.runner.MessageIds;

public class TestExecutionTimingProtocol {

	private record Timing(String testId, long start, long elapsed, long cpu, long user) {
	}

	private static class RecordingListener implements ITestRunListener2 {
		final List<Timing> timings= new ArrayList<>();
		final List<String> events= new ArrayList<>();

		@Override
		public void testTiming(String testId, long start, long elapsed, long cpu, long user) {
			timings.add(new Timing(testId, start, elapsed, cpu, user));
		}

		@Override
		public void testRunStarted(int testCount) {
			events.add("runStarted:" + testCount); //$NON-NLS-1$
		}

		@Override
		public void testRunEnded(long elapsedTime) {
			events.add("runEnded:" + elapsedTime); //$NON-NLS-1$
		}

		@Override
		public void testStarted(String testId, String testName) {
			events.add("started:" + testId); //$NON-NLS-1$
		}

		@Override
		public void testEnded(String testId, String testName) {
			events.add("ended:" + testId); //$NON-NLS-1$
		}

		@Override
		public void testFailed(int status, String testId, String testName, String trace, String expected, String actual) {
			events.add("failed:" + testId + ":" + status + ":" + trace); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		@Override
		public void testRunStopped(long elapsedTime) {
			events.add("stopped"); //$NON-NLS-1$
		}

		@Override
		public void testRunTerminated() {
			events.add("terminated"); //$NON-NLS-1$
		}

		@Override
		public void testTreeEntry(String description) {
		}

		@Override
		public void testReran(String testId, String testClass, String testName, int status, String trace, String expected, String actual) {
		}
	}

	private RemoteTestRunnerClient fClient;
	private RecordingListener fListener;
	private Method fReceiveMessage;

	@Before
	public void setUp() throws Exception {
		fClient= new RemoteTestRunnerClient(TestKindRegistry.getDefault().getKind(TestKindRegistry.JUNIT5_TEST_KIND_ID));
		fListener= new RecordingListener();
		// Exercise the socket reader's dispatcher synchronously, without opening a connection.
		Field listeners= RemoteTestRunnerClient.class.getDeclaredField("fListeners"); //$NON-NLS-1$
		listeners.setAccessible(true);
		listeners.set(fClient, new ITestRunListener2[] { fListener });
		fReceiveMessage= RemoteTestRunnerClient.class.getDeclaredMethod("receiveMessage", String.class); //$NON-NLS-1$
		fReceiveMessage.setAccessible(true);
	}

	@Test
	public void testInvalidTimingRangesAreIgnored() throws Exception {
		for (String timing : List.of(
				"1,-1,20,10,5", //$NON-NLS-1$
				"1,10,-1,10,5", //$NON-NLS-1$
				"1,10,20,-2,5", //$NON-NLS-1$
				"1,10,20,10,-2", //$NON-NLS-1$
				"1,-9223372036854775808,20,10,5", //$NON-NLS-1$
				"1,10,-9223372036854775808,10,5", //$NON-NLS-1$
				"1,10,20,-9223372036854775808,5", //$NON-NLS-1$
				"1,10,20,10,-9223372036854775808", //$NON-NLS-1$
				"1,9223372036854775807,1,-1,-1", //$NON-NLS-1$
				"1,1,9223372036854775807,-1,-1")) { //$NON-NLS-1$
			assertTimingIgnored(timing);
		}
	}

	@Test
	public void testMalformedTimingDoesNotInterruptResults() throws Exception {
		for (String timing : List.of(
				"", //$NON-NLS-1$
				",10,20,10,5", //$NON-NLS-1$
				"1,10,20,10", //$NON-NLS-1$
				"1,10,20,10,5,0", //$NON-NLS-1$
				"1,,20,10,5", //$NON-NLS-1$
				"1,10,20,10,", //$NON-NLS-1$
				"1,10,unknown,10,5", //$NON-NLS-1$
				"1,10,20,9223372036854775808,5")) { //$NON-NLS-1$
			assertTimingIgnored(timing);
		}
	}

	@Test
	public void testValidTimingBoundariesAndUnavailableCpuAreAccepted() throws Exception {
		List<Timing> timings= List.of(
				new Timing("1", 10, 20, 10, 5), //$NON-NLS-1$
				new Timing("1", 0, 0, 0, 0), //$NON-NLS-1$
				new Timing("1", 10, 20, -1, -1), //$NON-NLS-1$
				new Timing("1", 10, 20, 10, -1), //$NON-NLS-1$
				new Timing("1", Long.MAX_VALUE, 0, -1, -1), //$NON-NLS-1$
				new Timing("1", 0, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE)); //$NON-NLS-1$
		for (Timing timing : timings) {
			receive(MessageIds.TEST_TIMING + timing.testId() + ',' + timing.start() + ',' + timing.elapsed() + ',' + timing.cpu() + ',' + timing.user());
		}
		assertEquals(timings, fListener.timings);
	}

	@Test
	public void testLegacyResultsDoNotRequireTiming() throws Exception {
		assertTimingIgnored(null);
	}

	private void assertTimingIgnored(String timing) throws Exception {
		fListener.events.clear();
		receive(MessageIds.TEST_RUN_START + "1 v2"); //$NON-NLS-1$
		receive(MessageIds.TEST_START + "1,testTiming(pack.TimingTest)"); //$NON-NLS-1$
		if (timing != null)
			receive(MessageIds.TEST_TIMING + timing);
		receive(MessageIds.TEST_FAILED + "1,testTiming(pack.TimingTest)"); //$NON-NLS-1$
		receive(MessageIds.TRACE_START);
		receive("AssertionError"); //$NON-NLS-1$
		receive(MessageIds.TRACE_END);
		receive(MessageIds.TEST_END + "1,testTiming(pack.TimingTest)"); //$NON-NLS-1$
		receive(MessageIds.TEST_RUN_END + "23"); //$NON-NLS-1$
		assertEquals(timing, List.of("runStarted:1", "started:1", "failed:1:2:AssertionError", "ended:1", "runEnded:23"), fListener.events); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		assertTrue(timing, fListener.timings.isEmpty());
	}

	private void receive(String message) throws Exception {
		fReceiveMessage.invoke(fClient, message);
	}
}
