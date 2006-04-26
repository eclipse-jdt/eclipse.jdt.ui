/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     David Saff (saff@mit.edu) - initial API and implementation
 *             (bug 102632: [JUnit] Support for JUnit 4.)
 *******************************************************************************/

/**
 * 
 */
package org.eclipse.jdt.internal.junit.runner.junit3;

import org.eclipse.jdt.internal.junit.runner.ITestIdentifier;
import org.eclipse.jdt.internal.junit.runner.MessageIds;
import org.eclipse.jdt.internal.junit.runner.TestExecution;
import org.eclipse.jdt.internal.junit.runner.IListensToTestExecutions;
import org.eclipse.jdt.internal.junit.runner.TestReferenceFailure;
import org.eclipse.jdt.internal.junit.runner.IClassifiesThrowables;

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

	public void startTest(Test test) {
		fNotified.notifyTestStarted(id(test));
	}

	public void addError(Test test, Throwable throwable) {
		TestReferenceFailure failure= new TestReferenceFailure(id(test), MessageIds.TEST_ERROR, fClassifier.getTrace(throwable));
		fNotified.notifyTestFailed(failure);
	}

	public void addFailure(Test test, AssertionFailedError assertionFailedError) {
		newReference(test).sendFailure(assertionFailedError, fClassifier, fNotified);
	}

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
