/*******************************************************************************
 * Copyright (c) 2006, 2026 IBM Corporation and others.
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
 *     David Saff (saff@mit.edu) - initial API and implementation
 *             (bug 102632: [JUnit] Support for JUnit 4.)
 *******************************************************************************/

package org.eclipse.jdt.internal.junit.runner;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FirstRunExecutionListener implements IListensToTestExecutions {
	private static final long UNAVAILABLE_TIME= -1L;

	private static final class StartTiming {
		final long wallTimeNanos;
		final long cpuTimeNanos;
		final long userTimeNanos;
		final long threadId;

		StartTiming(long wallTimeNanos, long cpuTimeNanos, long userTimeNanos, long threadId) {
			this.wallTimeNanos= wallTimeNanos;
			this.cpuTimeNanos= cpuTimeNanos;
			this.userTimeNanos= userTimeNanos;
			this.threadId= threadId;
		}
	}

	protected static final class TestTiming {
		final long startTimeNanos;
		final long elapsedTimeNanos;
		final long cpuTimeNanos;
		final long userTimeNanos;

		TestTiming(long startTimeNanos, long elapsedTimeNanos, long cpuTimeNanos, long userTimeNanos) {
			this.startTimeNanos= startTimeNanos;
			this.elapsedTimeNanos= elapsedTimeNanos;
			this.cpuTimeNanos= cpuTimeNanos;
			this.userTimeNanos= userTimeNanos;
		}
	}

	protected MessageSender fSender;

	private final TestIdMap fIds;
	private final Map<String, StartTiming> fStartTimings= new ConcurrentHashMap<>();
	private final long fTimingOriginNanos= System.nanoTime();
	private final ThreadMXBean fThreadMXBean= ManagementFactory.getThreadMXBean();
	private final boolean fThreadCpuTimeEnabled;

	FirstRunExecutionListener(MessageSender sender, TestIdMap ids) {
		fSender= sender;
		if (ids == null)
			throw new NullPointerException();
		fIds= ids;
		fThreadCpuTimeEnabled= enableThreadCpuTime(fThreadMXBean);
	}

	private static boolean enableThreadCpuTime(ThreadMXBean threadMXBean) {
		if (!threadMXBean.isCurrentThreadCpuTimeSupported())
			return false;
		try {
			if (!threadMXBean.isThreadCpuTimeEnabled())
				threadMXBean.setThreadCpuTimeEnabled(true);
			return threadMXBean.isThreadCpuTimeEnabled();
		} catch (SecurityException | UnsupportedOperationException e) {
			return false;
		}
	}

	@Override
	public void notifyTestEnded(ITestIdentifier test) {
		String testId= getTestId(test);
		TestTiming timing= endTiming(testId);
		if (timing != null)
			sendTiming(testId, timing);
		sendMessage(testId, test, MessageIds.TEST_END);
		fSender.flush();
	}

	@Override
	public synchronized void notifyTestFailed(TestReferenceFailure failure) {
		sendMessage(failure.getTest(), failure.getStatus());
		sendFailure(failure, MessageIds.TRACE_START, MessageIds.TRACE_END);
		// fSender.flush(); // flush is implicitly done by sendFailure()
	}

	@Override
	public void notifyTestStarted(ITestIdentifier test) {
		String testId= getTestId(test);
		sendMessage(testId, test, MessageIds.TEST_START);
		fSender.flush();
		// Start after publishing TEST_START so socket/protocol latency is not charged to the test.
		startTiming(testId);
	}

	protected final void startTiming(String testId) {
		long wallTimeNanos= System.nanoTime();
		long cpuTimeNanos= currentThreadCpuTime();
		long userTimeNanos= currentThreadUserTime();
		fStartTimings.put(testId, new StartTiming(wallTimeNanos, cpuTimeNanos, userTimeNanos, Thread.currentThread().getId()));
	}

	protected final TestTiming endTiming(String testId) {
		StartTiming start= fStartTimings.remove(testId);
		if (start == null)
			return null;

		long endWallTimeNanos= System.nanoTime();
		long elapsedTimeNanos= Math.max(0L, endWallTimeNanos - start.wallTimeNanos);
		long cpuTimeNanos= UNAVAILABLE_TIME;
		long userTimeNanos= UNAVAILABLE_TIME;
		if (start.threadId == Thread.currentThread().getId()) {
			cpuTimeNanos= elapsedTime(start.cpuTimeNanos, currentThreadCpuTime());
			userTimeNanos= elapsedTime(start.userTimeNanos, currentThreadUserTime());
			if (cpuTimeNanos >= 0 && userTimeNanos > cpuTimeNanos)
				userTimeNanos= cpuTimeNanos;
		}
		return new TestTiming(start.wallTimeNanos - fTimingOriginNanos, elapsedTimeNanos, cpuTimeNanos, userTimeNanos);
	}

	private static long elapsedTime(long startTime, long endTime) {
		if (startTime < 0 || endTime < 0)
			return UNAVAILABLE_TIME;
		return Math.max(0L, endTime - startTime);
	}

	private long currentThreadCpuTime() {
		return fThreadCpuTimeEnabled ? fThreadMXBean.getCurrentThreadCpuTime() : UNAVAILABLE_TIME;
	}

	private long currentThreadUserTime() {
		return fThreadCpuTimeEnabled ? fThreadMXBean.getCurrentThreadUserTime() : UNAVAILABLE_TIME;
	}

	private String getTestId(ITestIdentifier test) {
		return fIds.getTestId(test);
	}

	protected void sendFailure(TestReferenceFailure failure, String startTrace,
			String endTrace) {
		FailedComparison comparison= failure.getComparison();
		if (comparison != null)
			comparison.sendMessages(fSender);

		fSender.sendMessage(startTrace);
		fSender.sendMessage(failure.getTrace());
		fSender.sendMessage(endTrace);
		fSender.flush();
	}

	protected final void sendTiming(String testId, TestTiming timing) {
		fSender.sendMessage(MessageIds.TEST_TIMING + testId + ',' + timing.startTimeNanos + ',' + timing.elapsedTimeNanos + ',' + timing.cpuTimeNanos + ',' + timing.userTimeNanos);
	}

	private void sendMessage(ITestIdentifier test, String status) {
		sendMessage(getTestId(test), test, status);
	}

	private void sendMessage(String testId, ITestIdentifier test, String status) {
		fSender.sendMessage(status + testId + ',' + RemoteTestRunner.escapeText(test.getName()));
	}

}
