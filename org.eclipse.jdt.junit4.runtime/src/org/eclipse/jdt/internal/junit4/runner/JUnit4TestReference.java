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
 *   David Saff (saff@mit.edu) - initial API and implementation
 *             (bug 102632: [JUnit] Support for JUnit 4.)
 *******************************************************************************/

package org.eclipse.jdt.internal.junit4.runner;

import java.util.List;

import org.junit.runner.Describable;
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

import junit.extensions.TestDecorator;
import junit.framework.Test;
import junit.framework.TestSuite;

public class JUnit4TestReference implements ITestReference {
	protected final Runner fRunner;

	protected final Description fRoot;
	private final ITestIdentifier fRootIdentifier;
	private final Test fLegacyTest;

	public JUnit4TestReference(Runner runner, Description root) {
		this(runner, root, null, null);
	}

	public JUnit4TestReference(Runner runner, Description root, Class<?> testClass, Test legacyTest) {
		fRunner= runner;
		fRoot= root;
		fLegacyTest= legacyTest;
		// Empty legacy suites and genuine TestCase leaves both have descriptions
		// without children. Use the actual Test, never a parsed display name.
		String className= testClass != null && isSuite(root, legacyTest) ? testClass.getName() : null;
		fRootIdentifier= new JUnit4Identifier(root, className);
	}

	private static boolean isSuite(Description description, Test test) {
		return unwrap(test) instanceof TestSuite || description.isSuite();
	}

	private static Test unwrap(Test test) {
		// JUnit uses a Describable decorator's own description; only ordinary
		// decorators have the same tree as the wrapped test.
		while (test instanceof TestDecorator && !(test instanceof Describable)) {
			test= ((TestDecorator) test).getTest();
		}
		return test;
	}

	@Override
	public int countTestCases() {
		return fLegacyTest != null ? fLegacyTest.countTestCases() : countTestCases(fRoot);
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
		return fRootIdentifier;
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
		sendTree(notified, fRoot, fLegacyTest);
	}

	private void sendTree(final IVisitsTestTrees notified, Description description, Test test) {
		ITestIdentifier identifier= description == fRoot ? fRootIdentifier : new JUnit4Identifier(description);
		if (!isSuite(description, test)) {
			notified.visitTreeEntry(identifier, false, 1, false, "-1"); //$NON-NLS-1$
		} else {
			List<Description> children= description.getChildren();
			notified.visitTreeEntry(identifier, true, children.size(), false, "-1"); //$NON-NLS-1$
			Test unwrapped= unwrap(test);
			TestSuite suite= unwrapped instanceof TestSuite ? (TestSuite) unwrapped : null;
			if (suite != null && suite.testCount() != children.size()) {
				suite= null;
			}
			for (int i= 0; i < children.size(); i++) {
				Test childTest= suite != null ? suite.testAt(i) : null;
				sendTree(notified, children.get(i), childTest);
			}
		}
	}

	@Override
	public String toString() {
		return fRoot.toString();
	}
}
