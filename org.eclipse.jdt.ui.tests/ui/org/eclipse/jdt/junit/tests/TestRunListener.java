/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.junit.tests;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.junit.ITestRunListener;

public class TestRunListener implements ITestRunListener {

	private static ArrayList/*<String>*/ fgLog= new ArrayList();
	
	public static void reset() {
		fgLog= new ArrayList();
	}
	
	public static List getMessages() {
		return fgLog;
	}

	public void testRunStarted(int testCount) {
		fgLog.add(testRunStartedMessage(testCount));
	}
	public static String testRunStartedMessage(int testCount) {
		return "testRunStarted(" + testCount + ")";
	}

	public void testRunEnded(long elapsedTime) {
		fgLog.add(testRunEndedMessage());
	}
	public static String testRunEndedMessage() {
		return "testRunEnded(" + ")";
	}
	
	public void testRunStopped(long elapsedTime) {
		fgLog.add(testRunStoppedMessage());
	}
	public static String testRunStoppedMessage() {
		return "testRunStopped(" + ")";
	}

	public void testStarted(String testId, String testName) {
		fgLog.add(testStartedMessage(testId, testName));
	}
	public static String testStartedMessage(String testId, String testName) {
		return "testStarted(" + testId + "," + testName + ")";
	}

	public void testEnded(String testId, String testName) {
		fgLog.add(testEndedMessage(testId, testName));
	}
	public static String testEndedMessage(String testId, String testName) {
		return "testEnded(" + testId + "," + testName + ")";
	}

	public void testFailed(int status, String testId, String testName, String trace) {
		fgLog.add(testFailedMessage(status, testId, testName));
	}
	public static String testFailedMessage(int status, String testId, String testName) {
		return "testFailed(" + status + "," + testId + "," + testName + ")";
	}

	public void testRunTerminated() {
		fgLog.add(testRunTerminatedMessage());
	}
	public static String testRunTerminatedMessage() {
		return "testRunTerminated(" + ")";
	}

	public void testReran(String testId, String testClass, String testName, int status, String trace) {
		fgLog.add(testReranMessage(testId, testClass, testName, status, trace));
	}
	public static String testReranMessage(String testId, String testClass, String testName, int status, String trace) {
		return "testFailed(" + testId + "," + testClass + "," + testName + "," + status + "," + escapeLinebreaks(trace) + ")";
	}

	private static String escapeLinebreaks(String trace) {
		return trace.replaceAll("\\r", "\\r").replaceAll("\\n", "\\n");
	}

}
