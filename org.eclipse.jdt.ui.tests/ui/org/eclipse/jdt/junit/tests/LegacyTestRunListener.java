/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.junit.tests;

import org.eclipse.jdt.junit.ITestRunListener;
import org.eclipse.jdt.junit.tests.AbstractTestRunListenerTest.TestRunLog;

/**
 * @deprecated
 */
public class LegacyTestRunListener implements ITestRunListener {

	private TestRunLog fLog;

	public LegacyTestRunListener(TestRunLog log) {
		fLog= log;
	}

	public void testRunStarted(int testCount) {
		if (fLog != null)
			fLog.add(testRunStartedMessage(testCount));
	}
	public static String testRunStartedMessage(int testCount) {
		return "testRunStarted(" + testCount + ")";
	}

	public void testRunEnded(long elapsedTime) {
		if (fLog != null) {
			fLog.add(testRunEndedMessage());
			fLog.setDone();
		}
	}
	public static String testRunEndedMessage() {
		return "testRunEnded(" + ")";
	}

	public void testRunStopped(long elapsedTime) {
		if (fLog != null) {
			fLog.add(testRunStoppedMessage());
			fLog.setDone();
		}
	}
	public static String testRunStoppedMessage() {
		return "testRunStopped(" + ")";
	}

	public void testStarted(String testId, String testName) {
		if (fLog != null)
			fLog.add(testStartedMessage(testId, testName));
	}
	public static String testStartedMessage(String testId, String testName) {
		return "testStarted(" + testId + "," + testName + ")";
	}

	public void testEnded(String testId, String testName) {
		if (fLog != null)
			fLog.add(testEndedMessage(testId, testName));
	}
	public static String testEndedMessage(String testId, String testName) {
		return "testEnded(" + testId + "," + testName + ")";
	}

	public void testFailed(int status, String testId, String testName, String trace) {
		if (fLog != null)
			fLog.add(testFailedMessage(status, testId, testName));
	}
	public static String testFailedMessage(int status, String testId, String testName) {
		return "testFailed(" + status + "," + testId + "," + testName + ")";
	}

	public void testRunTerminated() {
		if (fLog != null) {
			fLog.add(testRunTerminatedMessage());
			fLog.setDone();
		}
	}
	public static String testRunTerminatedMessage() {
		return "testRunTerminated(" + ")";
	}

	public void testReran(String testId, String testClass, String testName, int status, String trace) {
		if (fLog != null)
			fLog.add(testReranMessage(testId, testClass, testName, status, trace));
	}
	public static String testReranMessage(String testId, String testClass, String testName, int status, String trace) {
		return "testFailed(" + testId + "," + testClass + "," + testName + "," + status + "," + escapeLinebreaks(trace) + ")";
	}

	private static String escapeLinebreaks(String trace) {
		return trace.replaceAll("\\r", "\\r").replaceAll("\\n", "\\n");
	}

}
