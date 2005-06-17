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
package org.eclipse.jdt.ui.leaktest;

import junit.framework.TestCase;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.jdt.testplugin.util.DisplayHelper;



/**
 * Base class for leak test cases. To use the offered functionality, the test case has to run
 * in a LeakTestSetup that establishes the profile connection.
 */
public class LeakTestCase extends TestCase {
	
	public LeakTestCase(String name) {
		super(name);
	}
	
  	/**
   	 * Asserts that the instance count of the given class is as expected.
   	 * 
	 * @param clazz the class of the instances to count
  	 * @param expected the expected instance count
	 */
  	public static void assertInstanceCount(final Class clazz, final int expected) {
		final int[] count= new int[1];
		count[0]= -3;
		DisplayHelper helper= new DisplayHelper() {
			protected boolean condition() {
				count[0]= -2;
				System.gc();
				count[0]= -1;
				try {
					count[0]= getInstanceCount(clazz);
					System.out.println("instance count: " + count[0]);
				} catch (ProfileException e) {
					fail();
				}
				return count[0] == expected;
			}
		};
		boolean result= helper.waitForCondition(JavaPlugin.getActiveWorkbenchShell().getDisplay(), 60000);
		assertTrue("instance count is: " + count[0] + ", expected: " + expected, result);
	}
	
	/**
	 * Returns the number of instances of a given class that are live (not garbage).
	 * @param cl The class of the instances to count
	 * @return Returns the current number of instances of the given class or <code>-1</code> if
	 * no connection is established.
	 * @throws ProfileException ProfileException is thrown if the request failed unexpectedly.
	 */
	protected static int getInstanceCount(Class cl) throws ProfileException {
		ProfilerConnector connection= LeakTestSetup.getProfilerConnector();
		if (connection != null)
			return connection.getInstanceCount(cl);
		return -1;
	}
	
	/**
	 * Returns the number of instances of a given class that are live (not garbage).
	 * 
	 * @param cl The class of the instances to count
	 * @param excludedClasses the classes to exclude
	 * @return the current number of instances of the given class or <code>-1</code> if
	 * no connection is established.
	 * @throws ProfileException ProfileException is thrown if the request failed unexpectedly.
	 */
	protected static int getInstanceCount(Class cl, Class[] excludedClasses) throws ProfileException {
		ProfilerConnector connection= LeakTestSetup.getProfilerConnector();
		if (connection != null)
			return connection.getInstanceCount(cl, excludedClasses);
		
		return -1;
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
}
