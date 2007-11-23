/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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

import org.eclipse.swt.widgets.Display;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.testplugin.util.DisplayHelper;

import org.eclipse.jdt.ui.leaktest.reftracker.ReferenceTracker;


/**
 * Base class for leak test cases. 
 */
public class LeakTestCase extends TestCase {
	
	public LeakTestCase(String name) {
		super(name);
	}
	
	private InstancesOfTypeCollector collect(String requestedTypeName) {
		InstancesOfTypeCollector requestor= new InstancesOfTypeCollector(requestedTypeName, false);
		calmDown();
		new ReferenceTracker(requestor).start(getClass().getClassLoader());
		return requestor;
	}

	/*
	 * @see junit.framework.TestCase#setUp()
	 * @since 3.4
	 */
	protected void setUp() throws Exception {
		super.setUp();
		// Ensure active page to allow test being run
		IWorkbenchWindow activeWindow= PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (activeWindow.getActivePage() == null) {
			activeWindow.openPage(null);
		}
	}

	private void calmDown() {
		// Make sure we wait > 500, to allow e.g. TextViewer.queuePostSelectionChanged(boolean)
		// and OpenStrategy to time out and release references in delayed runnables.
		new DisplayHelper() {
			protected boolean condition() {
				return false;
			}
		}.waitForCondition(Display.getCurrent(), 1000);
		
		for (int i= 0; i < 10; i++) {
			System.gc();
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
			}
		}
	}
	
  	/**
   	 * Asserts that the instance count of the given class is as expected.
   	 * 
	 * @param clazz the class of the instances to count
  	 * @param expected the expected instance count
	 */
  	public void assertInstanceCount(final Class clazz, final int expected) {
  		int numTries= 2;
  		while (true) {
	  		InstancesOfTypeCollector requestor= collect(clazz.getName());
			int actual= requestor.getNumberOfResults();
			if (expected == actual) {
				return;
			}
			numTries--;
			if (numTries == 0) {
  				assertTrue("Expected: " + expected + ", actual: " + actual + "\n" + requestor.getResultString(), false);
			}
  		}
	}
	
	/**
	 * Returns the number of instances of a given class that are live (not garbage).
	 * @param clazz The class of the instances to count
	 * @return Returns the current number of instances of the given class or <code>-1</code> if
	 * no connection is established.
	 */
	protected int getInstanceCount(Class clazz) {
  		InstancesOfTypeCollector requestor= collect(clazz.getName());
		return requestor.getNumberOfResults();
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
		if (startCount == endCount) {
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
		if (startCount != endCount) {
			// only compare if connection could be established
			String str= message != null ? message + ": " : "";
			assertTrue(str + "instance count is not the same: (" + startCount + " / " + endCount + " )", false);
		}
	}
}
