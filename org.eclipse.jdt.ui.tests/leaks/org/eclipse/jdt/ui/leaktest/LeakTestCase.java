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
package org.eclipse.jdt.ui.leaktest;

import junit.framework.TestCase;



/**
 * Base class for leak test cases. To use the offered functionality, the test case has to run
 * in a LeakTestSetup that establishes the profile connection.
 */
public class LeakTestCase extends TestCase {
	
	public LeakTestCase(String name) {
		super(name);
	}

	
	/**
	 * Assert that two counts are different. The method does not fail if the profile connection
	 * has not established (e.g. because profiling is not supported on the given platform)
	 * @param startCount
	 * @param endCount
	 */
	protected void assertDifferentCount(int startCount, int endCount) {
		assertDifferentCount(null, startCount, endCount);
	}
	
	/**
	 * Assert that two counts are different. The method does not fail if the profile connection
	 * has not established (e.g. because profiling is not supported on the given platform)
	 * @param message Message to be printed if the test fails.
	 * @param startCount
	 * @param endCount
	 */
	protected void assertDifferentCount(String message, int startCount, int endCount) {
		ProfilerConnector connection= LeakTestSetup.getProfilerConnector();
		if (connection != null && (startCount == endCount)) {
			String str= message != null ? message + ": " : "";
			assertTrue(str + "instance count is not different: (" + startCount + " / " + endCount + " )", false);
		}
	}
	
	/**
	 * Assert that two counts are equal. The method does not fail if the profile connection
	 * has not established (e.g. because profiling is not supported on the given platform)
	 * @param startCount
	 * @param endCount
	 */
	protected void assertEqualCount(int startCount, int endCount) {
		assertEqualCount(null, startCount, endCount);
	}
	
	/**
	 * Assert that two counts are equal. The method does not fail if the profile connection
	 * has not established (e.g. because profiling is not supported on the given platform)
	 * @param message Message to be printed if the test fails.
	 * @param startCount
	 * @param endCount
	 */
	protected void assertEqualCount(String message, int startCount, int endCount) {
		ProfilerConnector connection= LeakTestSetup.getProfilerConnector();
		if (connection != null && (startCount != endCount)) {
			// only compare if connection could be established
			String str= message != null ? message + ": " : "";
			assertTrue(str + "instance count is not the same: (" + startCount + " / " + endCount + " )", false);
		}
	}
	
	/**
	 * Returns the number of instances of a given class that are live (not garbage).
	 * @param cl The class of the instances to count
	 * @return Returns the current number of instances of the given class or <code>-1</code> if
	 * no connection is established.
	 * @throws ProfileException ProfileException is thrown if the request failed unexpectedly.
	 */
	protected int getInstanceCount(Class cl) throws ProfileException {
		ProfilerConnector connection= LeakTestSetup.getProfilerConnector();
		if (connection != null) {
			return connection.getInstanceCount(cl);
		}
		return -1;
	}
	
	/**
	 * Returns the number of instances of a given class that are live (not garbage).
	 * @param cl The class of the instances to count
	 * @return Returns the current number of instances of the given class or <code>-1</code> if
	 * no connection is established.
	 * @throws ProfileException ProfileException is thrown if the request failed unexpectedly.
	 */
	protected int getInstanceCount(Class cl, Class[] excludedClasses) throws ProfileException {
		ProfilerConnector connection= LeakTestSetup.getProfilerConnector();
		if (connection != null) {
			return connection.getInstanceCount(cl, excludedClasses);
		}
		return -1;
	}
	
}
