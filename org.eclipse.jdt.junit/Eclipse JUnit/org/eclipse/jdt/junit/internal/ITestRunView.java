/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.junit.internal;

/**
 * A TestRunView is shown as a page in a tabbed folder.
 * It contributes the page contents and can return
 * the currently selected tests. A TestRunView is 
 * notified about the start and finish of a run.
 */
interface ITestRunView {
	
	/**
	 * Returns the name of the currently selected Test in the View
	 */
	public String getTestName();

	/**
	 * Activates the TestRunView
	 */
	public void activate();
	
	/**
	 * Informs that the suite is about to start 
	 */
	public void aboutToStart();

	/**
	 * Returns the name of the RunView
	 */
	public String getName();
	
	/**
	 * Sets the current Test in the View
	 */
	public void setSelectedTest(String testName);
	
	/**
	 * Updates the View after reRun Button has been pressed
	 */
	public void updateTest(String testName);
	
	/**
	 * called if the TestRunnerUI is disposed
	 */
	public void dispose();	
}