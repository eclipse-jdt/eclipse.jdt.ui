/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.junit.ui;


/**
 * A TestRunView is shown as a page in a tabbed folder.
 * It contributes the page contents and can return
 * the currently selected test.
 */
interface ITestRunView {
	/**
	 * Returns the name of the currently selected Test in the View
	 */
	public String getSelectedTestId();

	/**
	 * Activates the TestRunView
	 */
	public void activate();
	
	/**
	 * Sets the focus in the TestRunView
	 */
	public void setFocus();
	
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
	public void setSelectedTest(String testId);
	
	/**
	 * A test has ended
	 */
	public void endTest(String testId);
	
	/**
	 * The status of a test has changed
	 */
	public void testStatusChanged(TestRunInfo newInfo);
	/**
	 * A new tree entry got posted.
	 */
	public void newTreeEntry(String treeEntry);

}