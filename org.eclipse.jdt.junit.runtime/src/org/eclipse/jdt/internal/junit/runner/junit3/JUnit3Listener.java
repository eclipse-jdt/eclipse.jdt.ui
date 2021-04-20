/*******************************************************************************
 * Copyright (c) 2006, 2021 IBM Corporation and others.
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

package org.eclipse.jdt.internal.junit.runner.junit3;

import org.eclipse.jdt.internal.junit.runner.IClassifiesThrowables;
import org.eclipse.jdt.internal.junit.runner.IListensToTestExecutions;
import org.eclipse.jdt.internal.junit.runner.ITestIdentifier;
import org.eclipse.jdt.internal.junit.runner.MessageIds;
import org.eclipse.jdt.internal.junit.runner.TestExecution;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestListener;

public class JUnit3Listener implements TestListener {
	private final IListensToTestExecutions fNotified;

	private final IClassifiesThrowables fClassifier;

	public JUnit3Listener(TestExecution execution) {
		fNotified= execution.getListener();
		fClassifier= execution.getClassifier();
	}

	@Override
	public void startTest(Test test) {
		fNotified.notifyTestStarted(id(test));
	}

	@Override
	public void addError(Test test, Throwable throwable) {
		newReference(test).sendFailure(throwable, fClassifier, MessageIds.TEST_ERROR, fNotified);
	}

	@Override
	public void addFailure(Test test, AssertionFailedError assertionFailedError) {
		newReference(test).sendFailure(assertionFailedError, fClassifier, MessageIds.TEST_FAILED, fNotified);
	}

	@Override
	public void endTest(Test test) {
		fNotified.notifyTestEnded(id(test));
	}

	private ITestIdentifier id(Test test) {
		return newReference(test).getIdentifier();
	}

	private JUnit3TestReference newReference(Test test) {
		return new JUnit3TestReference(test);
	}
}
