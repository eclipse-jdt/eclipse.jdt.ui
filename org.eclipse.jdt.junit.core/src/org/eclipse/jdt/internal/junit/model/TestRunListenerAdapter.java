/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.model;

import org.eclipse.jdt.junit.TestRunListener;
import org.eclipse.jdt.junit.model.ITestCaseElement;

import org.eclipse.jdt.internal.junit.model.TestElement.Status;
import org.eclipse.jdt.internal.junit.JUnitCorePlugin;


/**
 * Notifier for the callback listener API {@link TestRunListener}.
 */
public class TestRunListenerAdapter implements ITestSessionListener {

	private final TestRunSession fSession;

	public TestRunListenerAdapter(TestRunSession session) {
		fSession= session;
	}

	private Object[] getListeners() {
		return JUnitCorePlugin.getDefault().getNewTestRunListeners().getListeners();
	}

	private void fireSessionStarted() {
		Object[] listeners= getListeners();
		for (int i= 0; i < listeners.length; i++) {
			((TestRunListener) listeners[i]).sessionStarted(fSession);
		}
	}

	private void fireSessionFinished() {
		Object[] listeners= getListeners();
		for (int i= 0; i < listeners.length; i++) {
			((TestRunListener) listeners[i]).sessionFinished(fSession);
		}
	}

	private void fireTestCaseStarted(ITestCaseElement testCaseElement) {
		Object[] listeners= getListeners();
		for (int i= 0; i < listeners.length; i++) {
			((TestRunListener) listeners[i]).testCaseStarted(testCaseElement);
		}
	}

	private void fireTestCaseFinished(ITestCaseElement testCaseElement) {
		Object[] listeners= getListeners();
		for (int i= 0; i < listeners.length; i++) {
			((TestRunListener) listeners[i]).testCaseFinished(testCaseElement);
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
