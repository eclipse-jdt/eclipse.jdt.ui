/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.model;

import org.eclipse.jdt.junit.ITestRunListener;

import org.eclipse.jdt.internal.junit.runner.MessageIds;


/**
 * Extends ITestRunListener with
 * <ul>
 * <li>a call back to trace the test contents, and</li>
 * <li>additional arguments to {@link #testFailed(int, String, String, String)}
 * and {@link #testReran(String, String, String, int, String)} with expected and
 * actual value. The replaced methods from {@link ITestRunListener} are <i>not</i>
 * called on {@link ITestRunListener2}</li>
 * </ul>
 */
public interface ITestRunListener2 extends ITestRunListener {

	/**
	 * Information about a member of the test suite that is about to be run. The
	 * format of the string is:
	 * 
	 * <pre>
	 *  testId,testName,isSuite,testcount
	 *  
	 *  testId: a unique id for the test
	 *  testName: the name of the test
	 *  isSuite: true or false depending on whether the test is a suite
	 *  testCount: an integer indicating the number of tests 
	 *  
	 *  Example: &quot;324968,testPass(junit.tests.MyTest),false,1&quot;
	 * </pre>
	 * 
	 * @param description a string describing a tree entry
	 * 
	 * @see MessageIds#TEST_TREE
	 */
	public void testTreeEntry(String description);

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
	public void testFailed(int status, String testId, String testName, String trace, String expected, String actual);

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
	public void testReran(String testId, String testClass, String testName, int status, String trace, String expected, String actual);
}
