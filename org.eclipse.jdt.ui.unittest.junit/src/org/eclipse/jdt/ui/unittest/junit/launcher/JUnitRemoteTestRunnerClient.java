/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *     Julien Ruaux: jruaux@octo.com
 * 	   Vincent Massol: vmassol@octo.com
 *******************************************************************************/
package org.eclipse.jdt.ui.unittest.junit.launcher;

import java.time.Duration;
import java.util.Arrays;

import org.eclipse.unittest.model.ITestCaseElement;
import org.eclipse.unittest.model.ITestElement;
import org.eclipse.unittest.model.ITestElement.FailureTrace;
import org.eclipse.unittest.model.ITestElement.Result;
import org.eclipse.unittest.model.ITestRunSession;
import org.eclipse.unittest.model.ITestSuiteElement;

import org.eclipse.core.runtime.ISafeRunnable;

import org.eclipse.debug.core.ILaunch;

import org.eclipse.jdt.internal.junit.runner.MessageIds;
import org.eclipse.jdt.internal.junit.runner.RemoteTestRunner;

import org.eclipse.jdt.ui.unittest.junit.JUnitTestPlugin;
import org.eclipse.jdt.ui.unittest.junit.internal.launcher.RemoteTestRunnerClient;

/**
 * The client side of the RemoteTestRunner. Handles the marshaling of the
 * different messages.
 */
public class JUnitRemoteTestRunnerClient extends RemoteTestRunnerClient {
	public JUnitRemoteTestRunnerClient(int port, ITestRunSession session) {
		super(port, session);
	}

	public abstract class ListenerSafeRunnable implements ISafeRunnable {
		@Override
		public void handleException(Throwable exception) {
			JUnitTestPlugin.log(exception);
		}
	}

	/**
	 * A simple state machine to process requests from the RemoteTestRunner
	 */
	abstract class ProcessingState {
		abstract ProcessingState readMessage(String message);
	}

	class DefaultProcessingState extends ProcessingState {
		@Override
		ProcessingState readMessage(String message) {
			if (fDebug) {
				System.out.println("JUnitRemoteTestRunnerClient.DefaultProcessingState.readMessage: " + message); //$NON-NLS-1$
			}

			if (message.startsWith(MessageIds.TRACE_START)) {
				fFailedTrace.setLength(0);
				return fTraceState;
			}
			if (message.startsWith(MessageIds.EXPECTED_START)) {
				fExpectedResult.setLength(0);
				return fExpectedState;
			}
			if (message.startsWith(MessageIds.ACTUAL_START)) {
				fActualResult.setLength(0);
				return fActualState;
			}
			if (message.startsWith(MessageIds.RTRACE_START)) {
				fFailedRerunTrace.setLength(0);
				return fRerunState;
			}
			String arg = message.substring(MessageIds.MSG_HEADER_LENGTH);
			if (message.startsWith(MessageIds.TEST_RUN_START)) {
				// version < 2 format: count
				// version >= 2 format: count+" "+version
				int count = 0;
				int v = arg.indexOf(' ');
				if (v == -1) {
					fVersion = "v1"; //$NON-NLS-1$
					count = Integer.parseInt(arg);
				} else {
					fVersion = arg.substring(v + 1);
					String sc = arg.substring(0, v);
					count = Integer.parseInt(sc);
				}
				fTestRunSession.notifyTestSessionStarted(count);
				return this;
			}
			if (message.startsWith(MessageIds.TEST_START)) {
				String s[] = extractTestId(arg);
				ITestElement test = fTestRunSession.getTestElement(s[0]);
				fTestRunSession.notifyTestStarted(test);
				return this;
			}
			if (message.startsWith(MessageIds.TEST_END)) {
				String s[] = extractTestId(arg);
				boolean isIgnored = s[1].startsWith(MessageIds.IGNORED_TEST_PREFIX);
				ITestElement testElement = fTestRunSession.getTestElement(s[0]);
				fTestRunSession.notifyTestEnded(testElement, isIgnored);
				return this;
			}
			if (message.startsWith(MessageIds.TEST_ERROR)) {
				String s[] = extractTestId(arg);
				ITestElement testElement = fTestRunSession.getTestElement(s[0]);
				boolean isAssumptionFailed = s[1].startsWith(MessageIds.ASSUMPTION_FAILED_TEST_PREFIX);
				extractFailure(testElement, Result.ERROR, isAssumptionFailed);
				return this;
			}
			if (message.startsWith(MessageIds.TEST_FAILED)) {
				String s[] = extractTestId(arg);
				ITestElement testElement = fTestRunSession.getTestElement(s[0]);
				boolean isAssumptionFailed = s[1].startsWith(MessageIds.ASSUMPTION_FAILED_TEST_PREFIX);
				extractFailure(testElement, Result.FAILURE, isAssumptionFailed);
				return this;
			}
			if (message.startsWith(MessageIds.TEST_RUN_END)) {
				fTestRunSession.notifyTestSessionCompleted(Duration.ofMillis(Long.parseLong(arg)));
				return this;
			}
			if (message.startsWith(MessageIds.TEST_STOPPED)) {
				fTestRunSession.notifyTestSessionAborted(Duration.ofMillis(Long.parseLong(arg)), null);
				shutDown();
				return this;
			}
			if (message.startsWith(MessageIds.TEST_TREE)) {
				notifyTestTreeEntry(arg);
				return this;
			}
			if (message.startsWith(MessageIds.TEST_RERAN)) {
				if (hasTestId())
					scanReranMessage(arg);
				else
					scanOldReranMessage(arg);
				return this;
			}
			return this;
		}
	}

	/**
	 * Base class for states in which messages are appended to an internal string
	 * buffer until an end message is read.
	 */
	class AppendingProcessingState extends ProcessingState {
		private final StringBuilder fBuffer;
		private String fEndString;

		AppendingProcessingState(StringBuilder buffer, String endString) {
			this.fBuffer = buffer;
			this.fEndString = endString;
		}

		@Override
		ProcessingState readMessage(String message) {
			if (message.startsWith(fEndString)) {
				entireStringRead();
				return fDefaultState;
			}
			fBuffer.append(message);
			if (fLastLineDelimiter != null)
				fBuffer.append(fLastLineDelimiter);
			return this;
		}

		/**
		 * subclasses can override to do special things when end message is read
		 */
		void entireStringRead() {
			// Nothing to do
		}
	}

	class TraceProcessingState extends AppendingProcessingState {
		TraceProcessingState() {
			super(fFailedTrace, MessageIds.TRACE_END);
		}

		@Override
		void entireStringRead() {
			fTestRunSession.notifyTestFailed(fFailedTest, fFailureKind, fFailedAssumption, new FailureTrace(
					fFailedTrace.toString(), nullifyEmpty(fExpectedResult), nullifyEmpty(fActualResult)));
			fExpectedResult.setLength(0);
			fActualResult.setLength(0);
		}

		@Override
		ProcessingState readMessage(String message) {
			if (message.startsWith(MessageIds.TRACE_END)) {
				fTestRunSession.notifyTestFailed(fFailedTest, fFailureKind, fFailedAssumption, new FailureTrace(
						fFailedTrace.toString(), nullifyEmpty(fExpectedResult), nullifyEmpty(fActualResult)));
				fFailedTrace.setLength(0);
				fActualResult.setLength(0);
				fExpectedResult.setLength(0);
				return fDefaultState;
			}
			fFailedTrace.append(message);
			if (fLastLineDelimiter != null)
				fFailedTrace.append(fLastLineDelimiter);
			return this;
		}

		/**
		 * Returns a comparison result from the given buffer. Removes the terminating
		 * line delimiter.
		 *
		 * @param buf the comparison result
		 * @return the result or <code>null</code> if empty
		 */
	}

	private ITestElement fFailedTest;
	/**
	 * The kind of failure of the test that is currently reported as failed
	 */
	private Result fFailureKind;
	/**
	 * Is Assumption failed on failed test
	 */
	private boolean fFailedAssumption;
	/**
	 * The failed trace that is currently reported from the RemoteTestRunner
	 */
	private final StringBuilder fFailedTrace = new StringBuilder();
	/**
	 * The expected test result
	 */
	private final StringBuilder fExpectedResult = new StringBuilder();
	/**
	 * The actual test result
	 */
	private final StringBuilder fActualResult = new StringBuilder();
	/**
	 * The failed trace of a reran test
	 */
	private final StringBuilder fFailedRerunTrace = new StringBuilder();
	private ITestSuiteElement currentSuite;

	ProcessingState fDefaultState = new DefaultProcessingState();
	ProcessingState fTraceState = new TraceProcessingState();
	ProcessingState fExpectedState = new AppendingProcessingState(fExpectedResult, MessageIds.EXPECTED_END);
	ProcessingState fActualState = new AppendingProcessingState(fActualResult, MessageIds.ACTUAL_END);
	ProcessingState fRerunState = new AppendingProcessingState(fFailedRerunTrace, MessageIds.RTRACE_END);
	ProcessingState fCurrentState = fDefaultState;

	/**
	 * An array of listeners that are informed about test events.
	 */
//	private ITestRunListener2[] fListeners;

	/**
	 * The server socket
	 */
	/*
	 * private ServerSocket fServerSocket; private Socket fSocket; private int
	 * fPort= -1; private PrintWriter fWriter; private PushbackReader
	 * fPushbackReader; private String fLastLineDelimiter;
	 */
	/**
	 * The protocol version
	 */
//	private String fVersion;

//	private boolean fDebug= false;

	/**
	 * Requests to stop the remote test run.
	 */
	@Override
	public synchronized void stopTest() {
		fWriter.println(MessageIds.TEST_STOP);
		fWriter.flush();
		ILaunch launch = fTestRunSession.getLaunch();
		try {
			launch.terminate();
		} catch (Exception ex) {
			JUnitTestPlugin.log(ex);
		}
	}

	@Override
	public void receiveMessage(String message) {
		fCurrentState = fCurrentState.readMessage(message);
	}

	private void scanOldReranMessage(String arg) {
		// OLD V1 format
		// format: className" "testName" "status
		// status: FAILURE, ERROR, OK
		int c = arg.indexOf(" "); //$NON-NLS-1$
		int t = arg.indexOf(" ", c + 1); //$NON-NLS-1$
		String className = arg.substring(0, c);
		String testName = arg.substring(c + 1, t);
		String status = arg.substring(t + 1);
		String testId = className + testName;
		notifyTestReran(testId, className, testName, status);
	}

	private void scanReranMessage(String arg) {
		// format: testId" "className" "testName" "status
		// status: FAILURE, ERROR, OK
		int i = arg.indexOf(' ');
		int c = arg.indexOf(' ', i + 1);
		int t; // special treatment, since testName can contain spaces:
		if (arg.endsWith(RemoteTestRunner.RERAN_ERROR)) {
			t = arg.length() - RemoteTestRunner.RERAN_ERROR.length() - 1;
		} else if (arg.endsWith(RemoteTestRunner.RERAN_FAILURE)) {
			t = arg.length() - RemoteTestRunner.RERAN_FAILURE.length() - 1;
		} else if (arg.endsWith(RemoteTestRunner.RERAN_OK)) {
			t = arg.length() - RemoteTestRunner.RERAN_OK.length() - 1;
		} else {
			t = arg.indexOf(' ', c + 1);
		}
		String testId = arg.substring(0, i);
		String className = arg.substring(i + 1, c);
		String testName = arg.substring(c + 1, t);
		String status = arg.substring(t + 1);
		notifyTestReran(testId, className, testName, status);
	}

	/*
	 * private void notifyTestReran(String testId, String className, String
	 * testName, String status) { int statusCode= ITestRunListener2.STATUS_OK; if
	 * (status.equals("FAILURE")) //$NON-NLS-1$ statusCode=
	 * ITestRunListener2.STATUS_FAILURE; else if (status.equals("ERROR"))
	 * //$NON-NLS-1$ statusCode= ITestRunListener2.STATUS_ERROR;
	 *
	 * String trace= ""; //$NON-NLS-1$ if (statusCode !=
	 * ITestRunListener2.STATUS_OK) trace = fFailedRerunTrace.toString(); //
	 * assumption a rerun trace was sent before notifyTestReran(testId, className,
	 * testName, statusCode, trace); }
	 *
	 * @Override protected void extractFailure(String testId, String testName, int
	 * status) { fFailedTestId= testId; fFailedTest= testName; fFailureKind= status;
	 * }
	 */
	/**
	 * @param arg test name
	 * @return an array with two elements. The first one is the testId, the second
	 *         one the testName.
	 */
	protected String[] extractTestId(String arg) {
		String[] result = new String[2];
		if (!hasTestId()) {
			result[0] = arg; // use the test name as the test Id
			result[1] = arg;
			return result;
		}
		int i = arg.indexOf(',');
		result[0] = arg.substring(0, i);
		result[1] = arg.substring(i + 1, arg.length());
		return result;
	}

	protected boolean hasTestId() {
		if (fVersion == null) // TODO fix me
			return true;
		return fVersion.equals("v2"); //$NON-NLS-1$
	}

	/*
	 * private void notifyTestReran(final String testId, final String className,
	 * final String testName, final int statusCode, final String trace) { for
	 * (ITestRunListener2 listener : fListeners) { SafeRunner.run(new
	 * ListenerSafeRunnable() {
	 *
	 * @Override public void run() { listener.testReran(testId, className, testName,
	 * statusCode, trace, nullifyEmpty(fExpectedResult),
	 * nullifyEmpty(fActualResult)); } }); } }
	 */
	private void notifyTestTreeEntry(final String treeEntry) {
		// format:
		// testId","testName","isSuite","testcount","isDynamicTest","parentId","displayName","parameterTypes","uniqueId
		String fixedTreeEntry = hasTestId() ? treeEntry : fakeTestId(treeEntry);

		int index0 = fixedTreeEntry.indexOf(',');
		String id = fixedTreeEntry.substring(0, index0);

		StringBuilder testNameBuffer = new StringBuilder(100);
		int index1 = scanTestName(fixedTreeEntry, index0 + 1, testNameBuffer);
		String testName = testNameBuffer.toString().trim();

		int index2 = fixedTreeEntry.indexOf(',', index1 + 1);
		boolean isSuite = fixedTreeEntry.substring(index1 + 1, index2).equals("true"); //$NON-NLS-1$

		int testCount;
//		boolean isDynamicTest;
		String parentId;
		String displayName;
		StringBuilder displayNameBuffer = new StringBuilder(100);
		String[] parameterTypes;
		StringBuilder parameterTypesBuffer = new StringBuilder(200);
		String uniqueId;
		StringBuilder uniqueIdBuffer = new StringBuilder(200);
		int index3 = fixedTreeEntry.indexOf(',', index2 + 1);
		if (index3 == -1) {
			testCount = Integer.parseInt(fixedTreeEntry.substring(index2 + 1));
//			isDynamicTest = false;
			parentId = null;
			displayName = null;
			parameterTypes = null;
			uniqueId = null;
		} else {
			testCount = Integer.parseInt(fixedTreeEntry.substring(index2 + 1, index3));

			int index4 = fixedTreeEntry.indexOf(',', index3 + 1);
//			isDynamicTest = Boolean.parseBoolean(fixedTreeEntry.substring(index3 + 1, index4));

			int index5 = fixedTreeEntry.indexOf(',', index4 + 1);
			parentId = fixedTreeEntry.substring(index4 + 1, index5);
			if (parentId.equals("-1")) { //$NON-NLS-1$
				parentId = null;
			}

			int index6 = scanTestName(fixedTreeEntry, index5 + 1, displayNameBuffer);
			displayName = displayNameBuffer.toString().trim();
			if (displayName.equals(testName)) {
				displayName = null;
			}

			int index7 = scanTestName(fixedTreeEntry, index6 + 1, parameterTypesBuffer);
			String parameterTypesString = parameterTypesBuffer.toString().trim();
			if (parameterTypesString.isEmpty()) {
				parameterTypes = null;
			} else {
				parameterTypes = parameterTypesString.split(","); //$NON-NLS-1$
				Arrays.parallelSetAll(parameterTypes, i -> parameterTypes[i].trim());
			}

			scanTestName(fixedTreeEntry, index7 + 1, uniqueIdBuffer);
			uniqueId = uniqueIdBuffer.toString().trim();
			if (uniqueId.isEmpty()) {
				uniqueId = null;
			}
		}

		ITestSuiteElement parent = getTestSuite(parentId);
		if (parent == null && currentSuite != null) {
			parent = currentSuite;
		}
		if (isSuite) {
			currentSuite = fTestRunSession.newTestSuite(id, testName, Integer.valueOf(testCount), parent, displayName,
					uniqueId);
		} else {
			fTestRunSession.newTestCase(id, testName, parent, displayName, uniqueId);
		}
	}

	private ITestSuiteElement getTestSuite(String parentId) {
		ITestElement element = fTestRunSession.getTestElement(parentId);
		return element instanceof ITestSuiteElement ? (ITestSuiteElement) element : null;
	}

	/**
	 * Append the test name from <code>s</code> to <code>testName</code>.
	 *
	 * @param s        the string to scan
	 * @param start    the offset of the first character in <code>s</code>
	 * @param testName the result
	 *
	 * @return the index of the next ','
	 */
	private int scanTestName(String s, int start, StringBuilder testName) {
		boolean inQuote = false;
		int i = start;
		for (; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '\\' && !inQuote) {
				inQuote = true;
				continue;
			} else if (inQuote) {
				inQuote = false;
				testName.append(c);
			} else if (c == ',')
				break;
			else
				testName.append(c);
		}
		return i;
	}

	private String fakeTestId(String treeEntry) {
		// extract the test name and add it as the testId
		int index0 = treeEntry.indexOf(',');
		String testName = treeEntry.substring(0, index0).trim();
		return testName + "," + treeEntry; //$NON-NLS-1$
	}

	private void extractFailure(ITestElement failedTest, Result status, boolean isAssumptionFailed) {
		fFailedTest = failedTest;
		fFailureKind = status;
		fFailedAssumption = isAssumptionFailed;
	}

	private void notifyTestReran(String testId, String className, String testName, String status) {
		Result statusCode = Result.OK;
		if (status.equals("FAILURE")) { //$NON-NLS-1$
			statusCode = Result.FAILURE;
		} else if (status.equals("ERROR")) { //$NON-NLS-1$
			statusCode = Result.ERROR;
		}

		String trace = ""; //$NON-NLS-1$
		if (statusCode != Result.OK)
			trace = fFailedRerunTrace.toString();
		// assumption a rerun trace was sent before

		ITestCaseElement element = fTestRunSession.newTestCase(testId, testName, null, testName, className);
		if (statusCode != Result.OK) {
			fTestRunSession.notifyTestFailed(element, statusCode, false,
					new FailureTrace(trace, nullifyEmpty(fExpectedResult), nullifyEmpty(fActualResult)));
		}
		fTestRunSession.notifyTestEnded(element, false);
	}

	private static String nullifyEmpty(StringBuilder buf) {
		int length = buf.length();
		if (length == 0)
			return null;

		char last = buf.charAt(length - 1);
		if (last == '\n') {
			if (length > 1 && buf.charAt(length - 2) == '\r')
				return buf.substring(0, length - 2);
			else
				return buf.substring(0, length - 1);
		} else if (last == '\r') {
			return buf.substring(0, length - 1);
		}
		return buf.toString();
	}
}
