/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
package org.eclipse.jdt.internal.junit.model;

import org.eclipse.jdt.junit.TestRunListener;
import org.eclipse.jdt.junit.model.ITestCaseElement;

import org.eclipse.core.runtime.ListenerList;

import org.eclipse.jdt.internal.junit.JUnitCorePlugin;
import org.eclipse.jdt.internal.junit.model.TestElement.Status;


/**
 * Notifier for the callback listener API {@link TestRunListener}.
 */
public class TestRunListenerAdapter implements ITestSessionListener {

	private final TestRunSession fSession;

	public TestRunListenerAdapter(TestRunSession session) {
		fSession= session;
	}

	private ListenerList<TestRunListener> getListenerList() {
		return JUnitCorePlugin.getDefault().getNewTestRunListeners();
	}

	private void fireSessionStarted() {
		for (TestRunListener listener : getListenerList()) {
			listener.sessionStarted(fSession);
		}
	}

	private void fireSessionFinished() {
		for (TestRunListener listener : getListenerList()) {
			listener.sessionFinished(fSession);
		}
	}

	private void fireTestCaseStarted(ITestCaseElement testCaseElement) {
		for (TestRunListener listener : getListenerList()) {
			listener.testCaseStarted(testCaseElement);
		}
	}

	private void fireTestCaseFinished(ITestCaseElement testCaseElement) {
		for (TestRunListener listener : getListenerList()) {
			listener.testCaseFinished(testCaseElement);
		}
	}


	@Override
	public void sessionStarted() {
		// wait until all test are added
	}

	@Override
	public void sessionEnded(long elapsedTime) {
		fireSessionFinished();
		fSession.swapOut();
	}

	@Override
	public void sessionStopped(long elapsedTime) {
		fireSessionFinished();
		fSession.swapOut();
	}

	@Override
	public void sessionTerminated() {
		fSession.swapOut();
	}

	@Override
	public void testAdded(TestElement testElement) {
		// do nothing
	}

	@Override
	public void runningBegins() {
		fireSessionStarted();
	}

	@Override
	public void testStarted(TestCaseElement testCaseElement) {
		fireTestCaseStarted(testCaseElement);
	}

	@Override
	public void testEnded(TestCaseElement testCaseElement) {
		fireTestCaseFinished(testCaseElement);
	}

	@Override
	public void testFailed(TestElement testElement, Status status, String trace, String expected, String actual) {
		// ignore
	}

	@Override
	public void testReran(TestCaseElement testCaseElement, Status status, String trace, String expectedResult, String actualResult) {
		// ignore
	}

	@Override
	public boolean acceptsSwapToDisk() {
		return true;
	}
}
