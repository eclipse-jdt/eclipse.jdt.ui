/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.jdt.internal.junit.model;

import org.eclipse.jdt.internal.junit.runner.MessageIds;


public interface ITestRunListener2 {

	/**
   	 * Status constant indicating that a test passed (constant value 0).
 	 */
 	int STATUS_OK= 0;
 	/**
	 * Status constant indicating that a test had an error an unanticipated
	 * exception (constant value 1).
 	 */
 	int STATUS_ERROR= 1;
 	/**
	 * Status constant indicating that a test failed an assertion
	 * (constant value 2).
	 */
 	int STATUS_FAILURE= 2;
 	/**
 	 * A test run has started.
 	 *
 	 * @param testCount the number of individual tests that will be run
 	 */
	void testRunStarted(int testCount);
	/**
 	 * A test run has ended.
	 *
	 * @param elapsedTime the total elapsed time of the test run
	 */
	void testRunEnded(long elapsedTime);
	/**
	 * A test run has been stopped prematurely.
	 *
 	 * @param elapsedTime the time elapsed before the test run was stopped
	 */
	void testRunStopped(long elapsedTime);
	/**
	 * An individual test has started.
	 *
	 * @param testId a unique Id identifying the test
	 * @param testName the name of the test that started
	 */
	void testStarted(String testId, String testName);
	/**
	 * An individual test has ended.
	 *
	 * @param testId a unique Id identifying the test
	 * @param testName the name of the test that ended
	 */
	void testEnded(String testId, String testName);


	/**
	 * The VM instance performing the tests has terminated.
	 */
	void testRunTerminated();

	/**
	 * Information about a member of the test suite that is about to be run. The format of the
	 * string is:
	 *
	 * <pre>
	 *  testId,testName,isSuite,testcount,isDynamicTest,parentId,displayName,parameterTypes,uniqueId
	 *
	 *  testId: a unique id for the test
	 *  testName: the name of the test
	 *  isSuite: true or false depending on whether the test is a suite
	 *  testCount: an integer indicating the number of tests
	 *  isDynamicTest: true or false
	 *  parentId: the unique testId of its parent if it is a dynamic test, otherwise can be "-1"
	 *  displayName: the display name of the test
	 *  parameterTypes: comma-separated list of method parameter types if applicable, otherwise an empty string
	 *  uniqueId: the unique ID of the test provided by JUnit launcher, otherwise an empty string
	 *
	 *  Example: 324968,testPass(junit.tests.MyTest),false,1,false,-1,A simple test case,&quot;&quot;,&quot;&quot;
	 * </pre>
	 *
	 * @param description a string describing a tree entry
	 *
	 * @see MessageIds#TEST_TREE
	 */
	void testTreeEntry(String description);

	/**
	 * An individual test has failed with a stack trace.
	 *
	 * @param status the outcome of the test; one of
	 *        {@link #STATUS_ERROR STATUS_ERROR} or
	 *        {@link #STATUS_FAILURE STATUS_FAILURE}
	 * @param testId a unique Id identifying the test
	 * @param testName the name of the test that failed
	 * @param trace the stack trace
	 * @param expected the expected value
	 * @param actual the actual value
	 */
	void testFailed(int status, String testId, String testName, String trace, String expected, String actual);

	/**
	 * An individual test has been rerun.
	 *
	 * @param testId a unique Id identifying the test
	 * @param testClass the name of the test class that was rerun
	 * @param testName the name of the test that was rerun
	 * @param status the outcome of the test that was rerun; one of
	 *        {@link #STATUS_OK}, {@link #STATUS_ERROR}, or
	 *        {@link #STATUS_FAILURE}
	 * @param trace the stack trace in the case of abnormal termination, or the
	 *        empty string if none
	 * @param expected the expected value in case of abnormal termination, or
	 *        the empty string if none
	 * @param actual the actual value in case of abnormal termination, or the
	 *        empty string if none
	 */
	void testReran(String testId, String testClass, String testName, int status, String trace, String expected, String actual);



}
