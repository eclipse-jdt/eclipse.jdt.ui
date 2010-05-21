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


	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.junit.model.ITestSessionListener#sessionStarted()
	 */
	public void sessionStarted() {
		// wait until all test are added
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.junit.model.ITestSessionListener#sessionEnded(long)
	 */
	public void sessionEnded(long elapsedTime) {
		fireSessionFinished();
		fSession.swapOut();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.junit.model.ITestSessionListener#sessionStopped(long)
	 */
	public void sessionStopped(long elapsedTime) {
		fireSessionFinished();
		fSession.swapOut();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.junit.model.ITestSessionListener#sessionTerminated()
	 */
	public void sessionTerminated() {
		fSession.swapOut();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.junit.model.ITestSessionListener#testAdded(org.eclipse.jdt.internal.junit.model.TestElement)
	 */
	public void testAdded(TestElement testElement) {
		// do nothing
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.junit.model.ITestSessionListener#runningBegins()
	 */
	public void runningBegins() {
		fireSessionStarted();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.junit.model.ITestSessionListener#testStarted(org.eclipse.jdt.internal.junit.model.TestCaseElement)
	 */
	public void testStarted(TestCaseElement testCaseElement) {
		fireTestCaseStarted(testCaseElement);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.junit.model.ITestSessionListener#testEnded(org.eclipse.jdt.internal.junit.model.TestCaseElement)
	 */
	public void testEnded(TestCaseElement testCaseElement) {
		fireTestCaseFinished(testCaseElement);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.junit.model.ITestSessionListener#testFailed(org.eclipse.jdt.internal.junit.model.TestElement, org.eclipse.jdt.internal.junit.model.TestElement.Status, java.lang.String, java.lang.String, java.lang.String)
	 */
	public void testFailed(TestElement testElement, Status status, String trace, String expected, String actual) {
		// ignore
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.junit.model.ITestSessionListener#testReran(org.eclipse.jdt.internal.junit.model.TestCaseElement, org.eclipse.jdt.internal.junit.model.TestElement.Status, java.lang.String, java.lang.String, java.lang.String)
	 */
	public void testReran(TestCaseElement testCaseElement, Status status, String trace, String expectedResult, String actualResult) {
		// ignore
	}

	public boolean acceptsSwapToDisk() {
		return true;
	}
}
