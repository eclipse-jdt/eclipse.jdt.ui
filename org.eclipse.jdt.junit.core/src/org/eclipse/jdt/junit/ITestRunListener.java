/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.junit;
  
/**
 * A listener interface for observing the execution of a test run.
 * <p>
 * Clients contributing to the org.eclipse.jdt.junit.testRunListener 
 * extension point implement this interface.
 * 
 * @since 2.1
 */
 public interface ITestRunListener {
 	/**
 	 * Test passed.
 	 * 
 	 * @see ITestRunListener#testFailed
 	 */
 	public static final int STATUS_OK= 0;
 	/**
 	 * Test had an error an unanticipated exception occurred.
 	 * 
 	 * @see ITestRunListener#testFailed
 	 */
 	public static final int STATUS_ERROR= 1;
 	/**
 	 * Test had a failure an assertion failed.
 	 * 
  	 * @see ITestRunListener#testFailed
	 */
 	public static final int STATUS_FAILURE= 2;
 	/**
 	 * A test run has started.
 	 * 
 	 * @param testCount the number of tests that will be run.
 	 */
	public void testRunStarted(int testCount);
	/**
	 * A test run ended.
	 *
	 * @param elapsedTime the elapsed time of the test run.
	 */
	public void testRunEnded(long elapsedTime);
	/**
	 * A test run was stopped before it ended.
	 */
	public void testRunStopped(long elapsedTime);
	/**
	 * A test started.
	 * 
	 * @param elapsedTime the elapsed time of the test until it got stopped.
	 */
	public void testStarted(String testName);
	/**
	 * A test ended.
	 * 
	 * @param testName the name of the test that has started
	 */
	public void testEnded(String testName);
	/**
	 * A test failed.
	 * 
	 * @param testName the name of the test that has ended.
	 * @param status the status of the test.
	 * @param trace the stack trace in the case of a failure.
	 */
	public void testFailed(int status, String testName, String trace);	
			
	/**
	 * The test runner VM has terminated.
	 * 
	 */
	public void testRunTerminated();
	
	/**
	 * A single test was reran.

	 * @param testClass the name of the test class.
	 * @param testName the name of the test.
	 * @param status the status of the run
	 * @param trace the stack trace in the case of a failure.
	 */
	public void testReran(String testClass, String testName, int status, String trace);
}


