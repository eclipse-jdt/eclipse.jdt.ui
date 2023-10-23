/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.leaktest;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import org.junit.Before;

import org.eclipse.jdt.testplugin.util.DisplayHelper;

import org.eclipse.swt.widgets.Display;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.ui.leaktest.reftracker.ReferenceTracker;
import org.eclipse.jdt.ui.leaktest.reftracker.ReferenceVisitor;
import org.eclipse.jdt.ui.leaktest.reftracker.ReferencedObject;

/**
 * Base class for leak test cases.
 */
public class LeakTestCase {
	public static class MulipleCollectorVisitor extends ReferenceVisitor {
		private final ReferenceVisitor[] fVisitors;

		public MulipleCollectorVisitor(ReferenceVisitor[] visitors) {
			fVisitors= visitors;
		}

		@Override
		public boolean visit(ReferencedObject object, Class<?> clazz, boolean firstVisit) {
			boolean visitChildren= false;
			for (ReferenceVisitor visitor : fVisitors) {
				boolean res= visitor.visit(object, clazz, firstVisit);
				visitChildren |= res;
			}
			return visitChildren;
		}

	}

	private InstancesOfTypeCollector collect(String requestedTypeName) {
		InstancesOfTypeCollector requestor= new InstancesOfTypeCollector(requestedTypeName, false);
		calmDown();
		new ReferenceTracker(requestor).start(getClass().getClassLoader());
		return requestor;
	}

	private InstancesOfTypeCollector[] collect(String[] requestedTypeNames) {
		final InstancesOfTypeCollector[] requestors= new InstancesOfTypeCollector[requestedTypeNames.length];
		for (int i= 0; i < requestors.length; i++) {
			requestors[i]= new InstancesOfTypeCollector(requestedTypeNames[i], false);
		}
		calmDown();
		ReferenceVisitor visitor= new ReferenceVisitor() {
			@Override
			public boolean visit(ReferencedObject object, Class<?> clazz, boolean firstVisit) {
				for (InstancesOfTypeCollector requestor : requestors) {
					requestor.visit(object, clazz, firstVisit);
				}
				return true;
			}
		};

		new ReferenceTracker(visitor).start(getClass().getClassLoader());
		return requestors;
	}

	/*
	 * @see junit.framework.TestCase#setUp()
	 * @since 3.4
	 */
	@Before
	public void setUp() throws Exception {
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
			@Override
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
  	public void assertInstanceCount(final Class<?> clazz, final int expected) {
  		int numTries= 2;
  		while (true) {
	  		InstancesOfTypeCollector requestor= collect(clazz.getName());
			int actual= requestor.getNumberOfResults();
			if (expected == actual) {
				return;
			}
			numTries--;
			assertNotEquals("Expected instance count: " + expected + ", actual: " + actual + "\n" + requestor.getResultString(), 0, numTries);
  		}
	}


  	/**
   	 * Asserts that the instance count of the given class is as expected.
   	 *
	 * @param classNames the types names of the instances to count
  	 * @param expected the expected instance count
	 */
  	public void assertInstanceCount(final String[] classNames, final int[] expected) {
  		int numTries= 2;
  		while (true) {
	  		InstancesOfTypeCollector[] requestors= collect(classNames);

	  		boolean success= true;
	  		for (int k= 0; success && k < requestors.length; k++) {
				if (expected[k] != requestors[k].getNumberOfResults()) {
					success= false;
				}
			}
	  		if (success)
	  			return;

	  		numTries--;
			if (numTries == 0) {
	  			StringBuilder buf= new StringBuilder();
		  		for (int k= 0; k < requestors.length; k++) {
					int actual= requestors[k].getNumberOfResults();
					if (expected[k] != actual) {
						buf.append("Expected instance count: " + expected[k] + ", actual: " + actual + "\n" + requestors[k].getResultString()).append("\n---------------------\n");
					}
				}
  				fail(buf.toString());
			}
  		}
	}

	/**
	 * Returns the number of instances of a given class that are live (not garbage).
	 * @param clazz The class of the instances to count
	 * @return Returns the current number of instances of the given class or <code>-1</code> if
	 * no connection is established.
	 */
	protected int getInstanceCount(Class<?> clazz) {
  		InstancesOfTypeCollector requestor= collect(clazz.getName());
		return requestor.getNumberOfResults();
	}

	/**
	 * Assert that two counts are different. The method does not fail if the profile connection has
	 * not established (e.g. because profiling is not supported on the given platform).
	 *
	 * @param startCount the start count
	 * @param endCount the end count
	 */
	protected void assertDifferentCount(int startCount, int endCount) {
		assertDifferentCount(null, startCount, endCount);
	}

	/**
	 * Assert that two counts are different. The method does not fail if the profile connection has
	 * not established (e.g. because profiling is not supported on the given platform)
	 *
	 * @param message Message to be printed if the test fails.
	 *
	 * @param startCount the start count
	 * @param endCount the end count
	 */
	protected void assertDifferentCount(String message, int startCount, int endCount) {
		if (startCount == endCount) {
			String str= message != null ? message + ": " : "";
			fail(str + "instance count is not different: (" + startCount + " / " + endCount + " )");
		}
	}

	/**
	 * Assert that two counts are equal. The method does not fail if the profile connection has not
	 * established (e.g. because profiling is not supported on the given platform).
	 *
	 * @param startCount the start count
	 * @param endCount the end count
	 */
	protected void assertEqualCount(int startCount, int endCount) {
		assertEqualCount(null, startCount, endCount);
	}

	/**
	 * Assert that two counts are equal. The method does not fail if the profile connection has not
	 * established (e.g. because profiling is not supported on the given platform).
	 *
	 * @param message Message to be printed if the test fails.
	 * @param startCount the start count
	 * @param endCount the end count
	 */
	protected void assertEqualCount(String message, int startCount, int endCount) {
		if (startCount != endCount) {
			// only compare if connection could be established
			String str= message != null ? message + ": " : "";
			fail(str + "instance count is not the same: (" + startCount + " / " + endCount + " )");
		}
	}
}
