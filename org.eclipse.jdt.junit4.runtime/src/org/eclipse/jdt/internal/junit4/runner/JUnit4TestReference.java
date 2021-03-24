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
 *   David Saff (saff@mit.edu) - initial API and implementation
 *             (bug 102632: [JUnit] Support for JUnit 4.)
 *******************************************************************************/

package org.eclipse.jdt.internal.junit4.runner;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runner.notification.StoppedByUserException;

import org.eclipse.jdt.internal.junit.runner.IStopListener;
import org.eclipse.jdt.internal.junit.runner.ITestIdentifier;
import org.eclipse.jdt.internal.junit.runner.ITestReference;
import org.eclipse.jdt.internal.junit.runner.IVisitsTestTrees;
import org.eclipse.jdt.internal.junit.runner.TestExecution;

public class JUnit4TestReference implements ITestReference {
	protected final Runner fRunner;

	protected final Description fRoot;

	public JUnit4TestReference(Runner runner, Description root) {
		fRunner= runner;
		fRoot= root;
	}

	@Override
	public int countTestCases() {
		return countTestCases(fRoot);
	}

	private int countTestCases(Description description) {
		if (description.isTest()) {
			return 1;
		} else {
			int result= 0;
			for (Description child : description.getChildren()) {
				result+= countTestCases(child);
			}
			return result;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof JUnit4TestReference))
			return false;

		JUnit4TestReference ref= (JUnit4TestReference)obj;
		return (ref.fRoot.equals(fRoot));
	}

	@Override
	public ITestIdentifier getIdentifier() {
		return new JUnit4Identifier(fRoot);
	}

	@Override
	public int hashCode() {
		return fRoot.hashCode();
	}

	@Override
	public void run(TestExecution execution) {
		final RunNotifier notifier= new RunNotifier();
		notifier.addListener(new JUnit4TestListener(execution.getListener()));
		execution.addStopListener(new IStopListener() {
			@Override
			public void stop() {
				notifier.pleaseStop();
			}
		});

		Result result= new Result();
		RunListener listener= result.createListener();
		notifier.addListener(listener);
		try {
			notifier.fireTestRunStarted(fRunner.getDescription());
			fRunner.run(notifier);
			notifier.fireTestRunFinished(result);
		} catch (StoppedByUserException e) {
			// not interesting, see https://bugs.eclipse.org/329498
		} finally {
			notifier.removeListener(listener);
		}
	}

	@Override
	public void sendTree(IVisitsTestTrees notified) {
		sendTree(notified, fRoot);
	}

	private void sendTree(final IVisitsTestTrees notified, Description description) {
		if (description.isTest()) {
			notified.visitTreeEntry(new JUnit4Identifier(description), false, 1, false, "-1"); //$NON-NLS-1$
		} else {
			notified.visitTreeEntry(new JUnit4Identifier(description), true, description.getChildren().size(), false, "-1"); //$NON-NLS-1$
			for (Description child : description.getChildren()) {
				sendTree(notified, child);
			}
		}
	}

	@Override
	public String toString() {
		return fRoot.toString();
	}
}
