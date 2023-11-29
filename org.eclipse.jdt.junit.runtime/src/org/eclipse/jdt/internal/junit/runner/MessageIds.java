/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.jdt.internal.junit.runner;

/**
 * Message identifiers for messages sent by the
 * RemoteTestRunner.
 *
 * @see RemoteTestRunner
 */
public class MessageIds {
	/**
	 * The header length of a message, all messages
	 * have a fixed header length
	 */
	public static final int MSG_HEADER_LENGTH= 8;

	/**
	 * Notification that a test trace has started.
	 * The end of the trace is signaled by a TRACE_END
	 * message. In between the TRACE_START and TRACE_END
	 * the stack trace is submitted as multiple lines.
	 */
	public static final String TRACE_START= "%TRACES "; //$NON-NLS-1$
	/**
	 * Notification that a trace ends.
	 */
	public static final String TRACE_END=   "%TRACEE "; //$NON-NLS-1$
	/**
	 * Notification that the expected result has started.
	 * The end of the expected result is signaled by a Trace_END.
	 */
	public static final String EXPECTED_START= "%EXPECTS"; //$NON-NLS-1$
	/**
	 * Notification that an expected result ends.
	 */
	public static final String EXPECTED_END=   "%EXPECTE"; //$NON-NLS-1$
	/**
	 * Notification that the expected result has started.
	 * The end of the expected result is signaled by a Trace_END.
	 */
	public static final String ACTUAL_START= "%ACTUALS"; //$NON-NLS-1$
	/**
	 * Notification that an expected result ends.
	 */
	public static final String ACTUAL_END=   "%ACTUALE"; //$NON-NLS-1$
	/**
	 * Notification that a trace for a reran test has started.
	 * The end of the trace is signaled by a RTrace_END
	 * message.
	 */
	public static final String RTRACE_START= "%RTRACES"; //$NON-NLS-1$
	/**
	 * Notification that a trace of a reran trace ends.
	 */
	public static final String RTRACE_END=  "%RTRACEE"; //$NON-NLS-1$
	/**
	 * Notification that a test run has started.
	 * MessageIds.TEST_RUN_START + testCount.toString + " " + version
	 */
	public static final String TEST_RUN_START=  "%TESTC  "; //$NON-NLS-1$
	/**
	 * Notification that a test has started.
	 * MessageIds.TEST_START + testID + "," + testName
	 */
	public static final String TEST_START=  "%TESTS  ";		 //$NON-NLS-1$
	/**
	 * Notification that a test has ended.
	 * TEST_END + testID + "," + testName
	 */
	public static final String TEST_END=    "%TESTE  ";		 //$NON-NLS-1$
	/**
	 * Notification that a test had an error.
	 * TEST_ERROR + testID + "," + testName.
	 * After the notification follows the stack trace.
	 */
	public static final String TEST_ERROR=  "%ERROR  ";		 //$NON-NLS-1$
	/**
	 * Notification that a test had a failure.
	 * TEST_FAILED + testID + "," + testName.
	 * After the notification follows the stack trace.
	 */
	public static final String TEST_FAILED= "%FAILED ";	 //$NON-NLS-1$
	/**
	 * Notification that a test run has ended.
	 * TEST_RUN_END + elapsedTime.toString().
	 */
	public static final String TEST_RUN_END="%RUNTIME";	 //$NON-NLS-1$
	/**
	 * Notification that a test run was successfully stopped.
	 */
	public static final String TEST_STOPPED="%TSTSTP "; //$NON-NLS-1$
	/**
	 * Notification that a test was reran.
	 * TEST_RERAN + testId + " " + testClass + " " + testName + STATUS.
	 * Status = "OK" or "FAILURE".
	 */
	public static final String TEST_RERAN=  "%TSTRERN"; //$NON-NLS-1$

	/**
	 * Notification about a test inside the test suite. <br>
	 * TEST_TREE + testId + "," + testName + "," + isSuite + "," + testcount + "," + isDynamicTest +
	 * "," + parentId + "," + displayName + "," + parameterTypes + "," + uniqueId <br>
	 * isSuite = "true" or "false" <br>
	 * isDynamicTest = "true" or "false" <br>
	 * parentId = the unique id of its parent if it is a dynamic test, otherwise can be "-1" <br>
	 * displayName = the display name of the test <br>
	 * parameterTypes = comma-separated list of method parameter types if applicable, otherwise an
	 * empty string <br>
	 * uniqueId = the unique ID of the test provided by JUnit launcher, otherwise an empty string
	 * <br>
	 * See: ITestRunListener2#testTreeEntry
	 */
	public static final String TEST_TREE= "%TSTTREE"; //$NON-NLS-1$
	/**
	 * Request to stop the current test run.
	 */
	public static final String TEST_STOP=	">STOP   "; //$NON-NLS-1$
	/**
	 * Request to rerun a test.
	 * TEST_RERUN + testId + " " + testClass + " "+testName
	 */
	public static final String TEST_RERUN=	">RERUN  "; //$NON-NLS-1$

	/**
	 * MessageFormat to encode test method identifiers:
	 * testMethod(testClass)
	 */
	public static final String TEST_IDENTIFIER_MESSAGE_FORMAT= "{0}({1})"; //$NON-NLS-1$

	/**
	 * Test identifier prefix for ignored tests.
	 */
	public static final String IGNORED_TEST_PREFIX= "@Ignore: "; //$NON-NLS-1$

	/**
	 * Test identifier prefix for tests with assumption failures.
	 */
	public static final String ASSUMPTION_FAILED_TEST_PREFIX= "@AssumptionFailure: "; //$NON-NLS-1$

	private MessageIds() {
	}
}


