/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.junit;
  
/**
 * A listener interface for observing the
 * execution of a test run.
 */
 public interface ITestRunListener {
 	/**
 	 * Test passed.
 	 */
 	public static final int STATUS_OK= 0;
 	/**
 	 * Test had an error.
 	 */
 	public static final int STATUS_ERROR= 1;
 	/**
 	 * Test had a failure.
 	 */
 	public static final int STATUS_FAILURE= 2;
 	/**
 	 * A test run has started
 	 */
	public void testRunStarted(int testCount);
	/**
	 * A test run ended.
	 */
	public void testRunEnded(long elapsedTime);
	/**
	 * A test run was stopped before it ended
	 */
	public void testRunStopped(long elapsedTime);
	/**
	 * A test started
	 */
	public void testStarted(String testName);
	/**
	 * A test ended
	 */
	public void testEnded(String testName);
	/**
	 * A test failed.
	 */
	public void testFailed(int status, String testName, String trace);		
	/**
	 * Add an entry to the tree.
	 * The format of the string is: 
	 * testName","isSuite","testcount
	 * Example: "testPass(junit.tests.MyTest),false,1"
	 */ 
	public void testTreeEntry(String entry);
	/**
	 * The test runner VM has terminated
	 */
	public void testRunTerminated();
	
	/**
	 * A test was reran.
	 */
	public void testReran(String testClass, String testName, int status, String trace);
}


