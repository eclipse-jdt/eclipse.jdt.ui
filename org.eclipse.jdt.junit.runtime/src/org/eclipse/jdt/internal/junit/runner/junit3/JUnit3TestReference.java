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
 *     Oliver Masutti <eclipse@masutti.ch> - [JUnit] JUnit3TestReference handles JUnit4TestAdapter incorrectly - https://bugs.eclipse.org/397747
 *******************************************************************************/

package org.eclipse.jdt.internal.junit.runner.junit3;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.internal.junit.runner.FailedComparison;
import org.eclipse.jdt.internal.junit.runner.IClassifiesThrowables;
import org.eclipse.jdt.internal.junit.runner.IListensToTestExecutions;
import org.eclipse.jdt.internal.junit.runner.IStopListener;
import org.eclipse.jdt.internal.junit.runner.ITestIdentifier;
import org.eclipse.jdt.internal.junit.runner.ITestReference;
import org.eclipse.jdt.internal.junit.runner.IVisitsTestTrees;
import org.eclipse.jdt.internal.junit.runner.MessageIds;
import org.eclipse.jdt.internal.junit.runner.TestExecution;
import org.eclipse.jdt.internal.junit.runner.TestReferenceFailure;

import junit.extensions.TestDecorator;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;

public class JUnit3TestReference implements ITestReference {

	private final Test fTest;

	public static Object getField(Object object, String fieldName) {
		Class<? extends Object> clazz= object.getClass();
		try {
			Field field= clazz.getDeclaredField(fieldName);
			field.setAccessible(true);
			return field.get(object);
		} catch (Exception e) {
			// fall through
		}
		return null;
	}

	public JUnit3TestReference(Test test) {
		if (test == null)
			throw new NullPointerException();
		this.fTest= test;
	}

	@Override
	public int countTestCases() {
		return fTest.countTestCases();
	}

	@Override
	public boolean equals(Object obj) {
		if (! (obj instanceof JUnit3TestReference))
			return false;

		JUnit3TestReference ref= (JUnit3TestReference) obj;
		return ref.fTest.equals(fTest);
	}

	@Override
	public int hashCode() {
		return fTest.hashCode();
	}

	public String getName() {
		if (isJUnit4TestCaseAdapter(fTest)) {
			Method method= (Method) callJUnit4GetterMethod(fTest, "getTestMethod"); //$NON-NLS-1$
			return MessageFormat.format(MessageIds.TEST_IDENTIFIER_MESSAGE_FORMAT, method.getName(), method.getDeclaringClass().getName());
		}
		if (fTest instanceof TestCase) {
			TestCase testCase= (TestCase) fTest;
			return MessageFormat.format(MessageIds.TEST_IDENTIFIER_MESSAGE_FORMAT, testCase.getName(), fTest.getClass().getName());
		}
		if (fTest instanceof TestSuite) {
			TestSuite suite= (TestSuite) fTest;
			if (suite.getName() != null)
				return suite.getName();
			return suite.getClass().getName();
		}
		if (fTest instanceof TestDecorator) {
			TestDecorator decorator= (TestDecorator) fTest;
			return decorator.getClass().getName();
		}
		if (isJUnit4TestSuiteAdapter(fTest)) {
			Class<?> testClass= (Class<?>) callJUnit4GetterMethod(fTest, "getTestClass"); //$NON-NLS-1$
			return testClass.getName();
		}
		return fTest.toString();
	}

	public Test getTest() {
		return fTest;
	}

	@Override
	public void run(TestExecution execution) {
		final TestResult testResult= new TestResult();
		testResult.addListener(new JUnit3Listener(execution));
		execution.addStopListener(new IStopListener() {
			@Override
			public void stop() {
				testResult.stop();
			}
		});
		TestResult tr= testResult;

		fTest.run(tr);
	}

	@Override
	public void sendTree(IVisitsTestTrees notified) {
		if (fTest instanceof TestDecorator) {
			TestDecorator decorator= (TestDecorator) fTest;
			notified.visitTreeEntry(getIdentifier(), true, 1, false, "-1"); //$NON-NLS-1$
			sendTreeOfChild(decorator.getTest(), notified);
		} else if (fTest instanceof TestSuite) {
			TestSuite suite= (TestSuite) fTest;
			notified.visitTreeEntry(getIdentifier(), true, suite.testCount(), false, "-1"); //$NON-NLS-1$
			for (int i= 0; i < suite.testCount(); i++) {
				sendTreeOfChild(suite.testAt(i), notified);
			}
		} else if (isJUnit4TestSuiteAdapter(fTest)) {
			List<?> tests= (List<?>) callJUnit4GetterMethod(fTest, "getTests"); //$NON-NLS-1$
			notified.visitTreeEntry(getIdentifier(), true, tests.size(), false, "-1"); //$NON-NLS-1$
			for (Iterator<?> iter= tests.iterator(); iter.hasNext();) {
				sendTreeOfChild((Test) iter.next(), notified);
			}
		} else {
			notified.visitTreeEntry(getIdentifier(), false, fTest.countTestCases(), false, "-1"); //$NON-NLS-1$
		}
	}

	void sendFailure(Throwable throwable, IClassifiesThrowables classifier, String status, IListensToTestExecutions notified) {
		TestReferenceFailure failure;
		try {
			failure= new TestReferenceFailure(getIdentifier(), status, classifier.getTrace(throwable));
			if (classifier.isComparisonFailure(throwable)) {
				// transmit the expected and the actual string
				Object expected= JUnit3TestReference.getField(throwable, "fExpected"); //$NON-NLS-1$
				Object actual= JUnit3TestReference.getField(throwable, "fActual"); //$NON-NLS-1$
				if (expected != null && actual != null) {
					failure.setComparison(new FailedComparison((String) expected, (String) actual));
				}
			}
		} catch (RuntimeException e) {
			StringWriter stringWriter= new StringWriter();
			e.printStackTrace(new PrintWriter(stringWriter));
			failure= new TestReferenceFailure(getIdentifier(), MessageIds.TEST_FAILED, stringWriter.getBuffer().toString(), null);
		}
		notified.notifyTestFailed(failure);
	}

	private Object callJUnit4GetterMethod(Test test, String methodName) {
		Object result;
		try {
			Method method= test.getClass().getMethod(methodName, new Class[0]);
			result= method.invoke(test, new Object[0]);
		} catch (Exception e) {
			e.printStackTrace(System.err);
			result= null;
		}
		return result;
	}

	private boolean isJUnit4TestCaseAdapter(Test test) {
		return "junit.framework.JUnit4TestCaseAdapter".equals(test.getClass().getName()); //$NON-NLS-1$
	}

	private boolean isJUnit4TestSuiteAdapter(Test test) {
		String name= test.getClass().getName();
		return name.endsWith("JUnit4TestAdapter") && name.startsWith("junit.framework"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void sendTreeOfChild(Test test, IVisitsTestTrees notified) {
		new JUnit3TestReference(test).sendTree(notified);
	}

	@Override
	public ITestIdentifier getIdentifier() {
		return new JUnit3Identifier(this);
	}
}
