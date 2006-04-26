/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   David Saff (saff@mit.edu) - initial API and implementation
 *             (bug 102632: [JUnit] Support for JUnit 4.)
 *******************************************************************************/

/**
 * 
 */
package org.eclipse.jdt.internal.junit4.runner;


import org.eclipse.jdt.internal.junit.runner.IListensToTestExecutions;
import org.eclipse.jdt.internal.junit.runner.ITestIdentifier;
import org.eclipse.jdt.internal.junit.runner.TestReferenceFailure;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

public class JUnit4TestListener extends RunListener {
	private final IListensToTestExecutions fNotified;

	public JUnit4TestListener(IListensToTestExecutions notified) {
		fNotified= notified;
	}

	@Override
	public void testStarted(Description plan) throws Exception {
		fNotified.notifyTestStarted(identifier(plan));
	}

	@Override
	public void testFailure(Failure failure) throws Exception {
		String status= new FailureException(failure.getException()).getStatus();
		fNotified.notifyTestFailed(new TestReferenceFailure(new JUnit4Identifier(failure.getDescription()), status, failure.getTrace()));
	}

	@Override
	public void testIgnored(Description plan) throws Exception {
		// do nothing
		// WANT: handle ignores in the UI
		// WANT: prioritization works for plug-in tests.
	}

	@Override
	public void testFinished(Description plan) throws Exception {
		fNotified.notifyTestEnded(identifier(plan));
	}

	private ITestIdentifier identifier(Description plan) {
		return new JUnit4Identifier(plan);
	}
}
