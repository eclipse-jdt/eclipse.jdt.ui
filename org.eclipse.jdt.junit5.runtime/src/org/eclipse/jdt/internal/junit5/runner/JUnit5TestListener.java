/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.internal.junit5.runner;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Optional;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestExecutionResult.Status;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.opentest4j.AssertionFailedError;
import org.opentest4j.MultipleFailuresError;
import org.opentest4j.ValueWrapper;

import org.eclipse.jdt.internal.junit.runner.FailedComparison;
import org.eclipse.jdt.internal.junit.runner.IListensToTestExecutions;
import org.eclipse.jdt.internal.junit.runner.ITestIdentifier;
import org.eclipse.jdt.internal.junit.runner.MessageIds;
import org.eclipse.jdt.internal.junit.runner.RemoteTestRunner;
import org.eclipse.jdt.internal.junit.runner.TestIdMap;
import org.eclipse.jdt.internal.junit.runner.TestReferenceFailure;

public class JUnit5TestListener implements TestExecutionListener {

	private final IListensToTestExecutions fNotified;

	private RemoteTestRunner fRemoteTestRunner;

	private TestPlan fTestPlan;

	public JUnit5TestListener(IListensToTestExecutions notified, RemoteTestRunner remoteTestRunner) {
		fNotified= notified;
		fRemoteTestRunner= remoteTestRunner;
	}

	@Override
	public void testPlanExecutionStarted(TestPlan testPlan) {
		fTestPlan= testPlan;
	}

	@Override
	public void testPlanExecutionFinished(TestPlan testPlan) {
		fTestPlan= null;
	}

	@Override
	public void executionStarted(TestIdentifier testIdentifier) {
		if (testIdentifier.isTest()) {
			fNotified.notifyTestStarted(getIdentifier(testIdentifier, false, false));
		}
	}

	@Override
	public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
		notifyIfNotSuccessful(testIdentifier, testExecutionResult);
		if (testIdentifier.isTest()) {
			fNotified.notifyTestEnded(getIdentifier(testIdentifier, false, false));
		}
	}

	private void notifyIfNotSuccessful(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
		Status result= testExecutionResult.getStatus();
		if (result != Status.SUCCESSFUL) {
			String trace= ""; //$NON-NLS-1$
			FailedComparison comparison= null;
			String status= MessageIds.TEST_FAILED;

			boolean assumptionFailed= result == Status.ABORTED;
			Optional<Throwable> throwableOp= testExecutionResult.getThrowable();
			if (throwableOp.isPresent()) {
				Throwable exception= throwableOp.get();
				trace= getTrace(exception);
				comparison= getFailedComparison(exception);
				status= (assumptionFailed || exception instanceof AssertionError) ? MessageIds.TEST_FAILED : MessageIds.TEST_ERROR;
			}

			ITestIdentifier identifier= getIdentifier(testIdentifier, false, assumptionFailed);
			fNotified.notifyTestFailed(new TestReferenceFailure(identifier, status, trace, comparison));
		}
	}

	private String getTrace(Throwable exception) {
		StringWriter stringWriter= new StringWriter();
		exception.printStackTrace(new PrintWriter(stringWriter));
		return stringWriter.getBuffer().toString();
	}

	private FailedComparison getFailedComparison(Throwable exception) {
		if (exception instanceof AssertionFailedError) {
			AssertionFailedError assertionFailedError= (AssertionFailedError) exception;
			ValueWrapper expected= assertionFailedError.getExpected();
			ValueWrapper actual= assertionFailedError.getActual();
			if (expected == null || actual == null) {
				return null;
			}
			return new FailedComparison(expected.getStringRepresentation(), actual.getStringRepresentation());
		}

		if (exception instanceof MultipleFailuresError) {
			return getComparisonForMultipleFailures(exception);
		}

		// Avoid reference to ComparisonFailure initially to avoid NoClassDefFoundError for ComparisonFailure when junit.jar is not on the build path
		String classname= exception.getClass().getName();
		if ("junit.framework.ComparisonFailure".equals(classname)) { //$NON-NLS-1$
			junit.framework.ComparisonFailure comparisonFailure= (junit.framework.ComparisonFailure) exception;
			return new FailedComparison(comparisonFailure.getExpected(), comparisonFailure.getActual());
		}
		if ("org.junit.ComparisonFailure".equals(classname)) { //$NON-NLS-1$
			org.junit.ComparisonFailure comparisonFailure= (org.junit.ComparisonFailure) exception;
			return new FailedComparison(comparisonFailure.getExpected(), comparisonFailure.getActual());
		}

		return null;
	}

	protected FailedComparison getComparisonForMultipleFailures(Throwable exception) {
		StringBuilder expectedStr= new StringBuilder();
		StringBuilder actualStr= new StringBuilder();
		String delimiter= "\n\n"; //$NON-NLS-1$
		List<Throwable> failures= ((MultipleFailuresError) exception).getFailures();
		for (Throwable assertionError : failures) {
			if (assertionError instanceof MultipleFailuresError) {
				FailedComparison failedComparison= getComparisonForMultipleFailures(assertionError);
				if(failedComparison == null) {
					return null;
				}
				String expected= failedComparison.getExpected();
				String actual= failedComparison.getActual();
				if (expected == null || actual == null) {
					return null;
				}
				expectedStr.append(expected);
				actualStr.append(actual);
			} else if (assertionError instanceof AssertionFailedError) {
				AssertionFailedError assertionFailedError= (AssertionFailedError) assertionError;
				ValueWrapper expected= assertionFailedError.getExpected();
				ValueWrapper actual= assertionFailedError.getActual();
				if (expected == null || actual == null) {
					return null;
				}
				expectedStr.append(expected.getStringRepresentation()).append(delimiter);
				actualStr.append(actual.getStringRepresentation()).append(delimiter);
			} else {
				return null;
			}
		}
		return new FailedComparison(expectedStr.toString(), actualStr.toString());
	}

	@Override
	public void executionSkipped(TestIdentifier testIdentifier, String reason) {
		if (testIdentifier.isContainer() && fTestPlan != null) {
			fTestPlan.getDescendants(testIdentifier).stream().filter(TestIdentifier::isTest).forEachOrdered(this::notifySkipped);
		} else {
			notifySkipped(testIdentifier);
		}
	}

	private void notifySkipped(TestIdentifier testIdentifier) {
		// Send message to listeners which would be stale otherwise
		ITestIdentifier identifier= getIdentifier(testIdentifier, true, false);
		fNotified.notifyTestStarted(identifier);
		fNotified.notifyTestEnded(identifier);
	}


	@Override
	public void dynamicTestRegistered(TestIdentifier testIdentifier) {
		if (fTestPlan != null) {
			JUnit5Identifier dynamicTestIdentifier= new JUnit5Identifier(testIdentifier);
			boolean hasChildren;
			int testCount;
			if (testIdentifier.isContainer()) {
				hasChildren= true;
				testCount= fTestPlan.getChildren(testIdentifier).size();
			} else {
				hasChildren= false;
				testCount= 1;
			}
			String parentId= getParentId(testIdentifier, fTestPlan);
			fRemoteTestRunner.visitTreeEntry(dynamicTestIdentifier, hasChildren, testCount, true, parentId);
		}
	}

	/**
	 * @param testIdentifier the test identifier whose parent id is required
	 * @param testPlan the test plan containing the test
	 * @return the parent id from {@link TestIdMap} if the parent is present, otherwise
	 *         <code>"-1"</code>
	 */
	private String getParentId(TestIdentifier testIdentifier, TestPlan testPlan) {
		// Same as JUnit5TestReference.getParentId(TestIdentifier testIdentifier, TestPlan testPlan).
		return testPlan.getParent(testIdentifier).map(parent -> fRemoteTestRunner.getTestId(new JUnit5Identifier(parent))).orElse("-1"); //$NON-NLS-1$
	}

	private ITestIdentifier getIdentifier(TestIdentifier testIdentifier, boolean ignored, boolean assumptionFailed) {
		if (ignored) {
			return new IgnoredTestIdentifier(testIdentifier);
		}
		if (assumptionFailed) {
			return new AssumptionFailedTestIdentifier(testIdentifier);
		}
		return new JUnit5Identifier(testIdentifier);
	}

	private static class IgnoredTestIdentifier extends JUnit5Identifier {
		public IgnoredTestIdentifier(TestIdentifier description) {
			super(description);
		}

		@Override
		public String getName() {
			String name= super.getName();
			if (name != null)
				return MessageIds.IGNORED_TEST_PREFIX + name;
			return null;
		}
	}

	private static class AssumptionFailedTestIdentifier extends JUnit5Identifier {
		public AssumptionFailedTestIdentifier(TestIdentifier description) {
			super(description);
		}

		@Override
		public String getName() {
			String name= super.getName();
			if (name != null)
				return MessageIds.ASSUMPTION_FAILED_TEST_PREFIX + name;
			return null;
		}
	}

	@Override
	public void reportingEntryPublished(TestIdentifier testIdentifier, ReportEntry entry) {
		System.out.println(TestIdentifier.class.getSimpleName() + " [" + testIdentifier.getDisplayName() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
		System.out.println(entry);
	}
}
