/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.junit;
  
/**
 * A listener interface for observing the execution of a test run.
 * <p>
 * Clients contributing to the 
 * <code>org.eclipse.jdt.junit.testRunListener</code>
 * extension point implement this interface.
 * </p>
 * 
 * @since 2.1
 */
 public interface ITestRunListener {
	/**
   	 * Status constant indicating that a test passed (constant value 0).
 	 * 
     * @see #testFailed(int, String, String, String)
 	 */
 	public static final int STATUS_OK= 0;
 	/**
	 * Status constant indicating that a test had an error an unanticipated
	 * exception (constant value 1).
 	 * 
	 * @see #testFailed(int, String, String, String)
 	 */
 	public static final int STATUS_ERROR= 1;
 	/**
	 * Status constant indicating that a test failed an assertion
	 * (constant value 2).
 	 * 
 	 * @see #testFailed(int, String, String, String)
	 */
 	public static final int STATUS_FAILURE= 2;
 	/**
 	 * A test run has started.
 	 * 
 	 * @param testCount the number of individual tests that will be run
 	 */
	public void testRunStarted(int testCount);
	/**
 	 * A test run has ended.
	 *
	 * @param elapsedTime the total elapsed time of the test run
	 */
	public void testRunEnded(long elapsedTime);
	/**
	 * A test run has been stopped prematurely.
	 *
 	 * @param elapsedTime the time elapsed before the test run was stopped
	 */
	public void testRunStopped(long elapsedTime);
	/**
	 * An individual test has started.
	 * 
	 * @param testId a unique Id identifying the test
	 * @param testName the name of the test that started
	 */
	public void testStarted(String testId, String testName);
	/**
	 * An individual test has ended.
	 * 
	 * @param testId a unique Id identifying the test
	 * @param testName the name of the test that ended
	 */
	public void testEnded(String testId, String testName);
	/**
	 * An individual test has failed with a stack trace.
	 * 
	 * @param testId a unique Id identifying the test
 	 * @param testName the name of the test that failed
	 * @param status the outcome of the test; one of 
	 * {@link #STATUS_ERROR STATUS_ERROR} or
	 * {@link #STATUS_FAILURE STATUS_FAILURE}
	 * @param trace the stack trace
	 */
	public void testFailed(int status, String testId, String testName, String trace);	
			
	/**
	 * The VM instance performing the tests has terminated.
	 */
	public void testRunTerminated();
	
	/**
 	 * An individual test has been rerun.
	 * 
	 * @param testId a unique Id identifying the test
	 * @param testClass the name of the test class that was rerun
	 * @param testName the name of the test that was rerun
	 * @param status the outcome of the test that was rerun; one of 
	 * {@link #STATUS_OK STATUS_OK}, {@link #STATUS_ERROR STATUS_ERROR},
	 * or {@link #STATUS_FAILURE STATUS_FAILURE}
	 * @param trace the stack trace in the case of abnormal termination,
	 * or the empty string if none
	 */
	public void testReran(String testId, String testClass, String testName, int status, String trace);
}


