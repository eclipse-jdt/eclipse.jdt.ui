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
	 * Informs that the suite is about to start 
	 */
	public void aboutToEnd();
	
	/**
	 * Returns the name of the RunView
	 */
	public String getName();
	
	/**
	 * Sets the current Test in the View
	 */
	public void setSelectedTest(String testId);
	
	/**
	 * A test has started
	 */
	public void startTest(String testId);

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
	
	/**
	 * Select next test failure.
	 */
	public void selectNext();	
	
	/**
	 * Select previous test failure.
	 */
	public void selectPrevious();	
}
