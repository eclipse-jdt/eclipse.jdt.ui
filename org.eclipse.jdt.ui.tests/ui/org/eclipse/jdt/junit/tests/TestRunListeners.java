/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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

package org.eclipse.jdt.junit.tests;

import org.eclipse.jdt.junit.TestRunListener;
import org.eclipse.jdt.junit.model.ITestCaseElement;
import org.eclipse.jdt.junit.model.ITestElement;
import org.eclipse.jdt.junit.model.ITestElement.FailureTrace;
import org.eclipse.jdt.junit.model.ITestElement.ProgressState;
import org.eclipse.jdt.junit.model.ITestElement.Result;
import org.eclipse.jdt.junit.model.ITestElementContainer;
import org.eclipse.jdt.junit.model.ITestRunSession;
import org.eclipse.jdt.junit.model.ITestSuiteElement;
import org.eclipse.jdt.junit.tests.AbstractTestRunListenerTest.TestRunLog;


public class TestRunListeners {

	public static class SequenceTest extends TestRunListener {
		private final TestRunLog fLog;
		public SequenceTest(TestRunLog log) {
			fLog= log;
		}
		@Override
		public void sessionStarted(ITestRunSession session) {
			fLog.add("sessionStarted-" + asString(session, 0));
		}
		@Override
		public void sessionFinished(ITestRunSession session) {
			fLog.add("sessionFinished-" + asString(session, 0));
			fLog.setDone();
		}
		@Override
		public void testCaseStarted(ITestCaseElement testCaseElement) {
			fLog.add("testCaseStarted-" + asString(testCaseElement, 0));
		}
		@Override
		public void testCaseFinished(ITestCaseElement testCaseElement) {
			fLog.add("testCaseFinished-" + asString(testCaseElement, 0));
		}
	}

	public static class TreeTest extends TestRunListener {
		private final TestRunLog fLog;
		private int fStep;
		public TreeTest(TestRunLog log, int step) {
			fLog= log;
			fStep= step;
		}
		private void process(ITestElement elem) {
			if (--fStep == 0) {
				logElement(elem.getTestRunSession(), 0);
			}
		}

		private void logElement(ITestElement elem, int indent) {
			fLog.add(asString(elem, indent));
			if (elem instanceof ITestElementContainer) {
				for (ITestElement child : ((ITestElementContainer) elem).getChildren()) {
					logElement(child, indent + 1);
				}
			}
		}


		@Override
		public void sessionStarted(ITestRunSession session) {
			process(session);
		}
		@Override
		public void sessionFinished(ITestRunSession session) {
			process(session);
			if (fLog.getLog().length == 0) {
				fLog.add("""
					<empty>
					
					To see why the test log finished early, try to reduce
					the step number passed to new TreeTest(..., step).""");
			}
			fLog.setDone();
		}
		@Override
		public void testCaseStarted(ITestCaseElement testCaseElement) {
			process(testCaseElement);
		}
		@Override
		public void testCaseFinished(ITestCaseElement testCaseElement) {
			process(testCaseElement);
		}
	}

	public static String sessionAsString(String sessionName, ProgressState state, Result result, int indent) {
		StringBuilder buf= new StringBuilder();
		buf.append(startIndent(indent));
		buf.append("sessionName: ").append(sessionName).append(separator(indent));
		buf.append("state: ").append(state).append(separator(indent));
		buf.append("result: ").append(result).append(separator(indent));
		return buf.toString();
	}

	private static StringBuffer separator(int indent) {
		StringBuffer buf= new StringBuffer();
		buf.append('\n');
		for (int i= 0; i < indent + 1; i++) {
			buf.append("    ");
		}
		return buf;
	}

	private static StringBuffer startIndent(int indent) {
		StringBuffer buf= new StringBuffer();
		for (int i= 0; i < indent; i++) {
			buf.append("    ");
		}
		return buf;
	}

	public static String testCaseAsString(String methodName, String className, ProgressState state, Result result, FailureTrace trace, int indent) {
		StringBuilder buf= new StringBuilder();
		buf.append(startIndent(indent));
		buf.append("testCaseMethod: ").append(methodName).append(separator(indent));
		buf.append("class: ").append(className).append(separator(indent));
		buf.append("state: ").append(state).append(separator(indent));
		buf.append("result: ").append(result).append(separator(indent));
		if (trace != null) {
			buf.append("exception: ").append(getExpection(trace.getTrace())).append(separator(indent));
			buf.append("actual: ").append(trace.getActual()).append(separator(indent));
			buf.append("expected: ").append(trace.getExpected()).append(separator(indent));
		}
		return buf.toString();
	}

	public static String suiteAsString(String suiteName, ProgressState state, Result result, FailureTrace trace, int indent) {
		StringBuilder buf= new StringBuilder();
		buf.append(startIndent(indent));
		buf.append("testSuiteClass: ").append(suiteName).append(separator(indent));
		buf.append("state: ").append(state).append(separator(indent));
		buf.append("result: ").append(result).append(separator(indent));
		if (trace != null) {
			buf.append("exception: ").append(getExpection(trace.getTrace())).append(separator(indent));
			buf.append("actual: ").append(trace.getActual()).append(separator(indent));
			buf.append("expected: ").append(trace.getExpected()).append(separator(indent));
		}
		return buf.toString();
	}

	private static String getExpection(String trace) {
		if (trace == null) {
			return "null";
		}
		for (int i= 0; i < trace.length(); i++) {
			char ch= trace.charAt(i);
			if (!Character.isJavaIdentifierPart(ch) && ch != '.') {
				return trace.substring(0, i);
			}
		}
		return trace;
	}

	public static String asString(ITestElement elem, int indent) {
		if (elem instanceof ITestRunSession) {
			ITestRunSession session= (ITestRunSession) elem;
			return sessionAsString(session.getTestRunName(), session.getProgressState(), session.getTestResult(true), indent);
		} else if (elem instanceof ITestCaseElement) {
			ITestCaseElement testCaseElement= (ITestCaseElement) elem;
			return testCaseAsString(testCaseElement.getTestMethodName(), testCaseElement.getTestClassName(), testCaseElement.getProgressState(), testCaseElement.getTestResult(true), testCaseElement.getFailureTrace(), indent);
		} else {
			ITestSuiteElement testSuite= (ITestSuiteElement) elem;
			return suiteAsString(testSuite.getSuiteTypeName(), testSuite.getProgressState(), testSuite.getTestResult(true), testSuite.getFailureTrace(), indent);
		}
	}
}
