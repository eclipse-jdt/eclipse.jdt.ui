/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.junit.tests;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.ui.PartInitException;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.junit.ui.ITraceDisplay;
import org.eclipse.jdt.internal.junit.ui.TextualTrace;

public class WrappingUnitTest extends TestCase {
	public void test00wrapSecondLine() throws Exception {
		TextualTrace trace = new TextualTrace("12345\n1234512345",
				new String[0]);
		trace.display(new ITraceDisplay() {
			public void addTraceLine(int lineType, String label) {
				assertEquals("12345", label);
			}
		}, 5);
	}

	public void test01wrappingSystemTestRunTestsWaitsForCorrectNumberOfLines()
			throws Exception {
		final boolean[] wasCalled = { false };
		WrappingSystemTest test = new WrappingSystemTest() {
			protected void launchTests(String prefixForErrorMessage,
					int howManyNumbersInErrorString) throws CoreException,
					JavaModelException {
				// do nothing
			}

			protected void waitForTableToFill(int numExpectedTableLines,
					int millisecondTimeout, boolean lastItemHasImage) throws PartInitException {
				wasCalled[0] = true;
				assertEquals(17, numExpectedTableLines);
			}
		};

		test.runTests(null, 0, 17, false);
		assertTrue(wasCalled[0]);
	}

	public void test02waitForTableToFillWaitsForNumberOfLines()
			throws Exception {
		WrappingSystemTest test = new WrappingSystemTest() {
			protected boolean stillWaiting(int numExpectedTableLines, boolean lastItemHasImage)
					throws PartInitException {
				assertEquals(17, numExpectedTableLines);
				return false;
			}
		};
		test.waitForTableToFill(17, 30000, false);
	}

	public void test03waitForTableToFillObeysTimeout() throws Exception {
		final WrappingSystemTest test = new WrappingSystemTest() {
			protected void dispatchEvents() {
				// do nothing (avoid accessing display from non-UI thread)
			}

			protected int getNumTableItems() throws PartInitException {
				return -1; // avoid accessing getActiveWorkbenchWindow() from non-UI thread
			}

			protected boolean stillWaiting(int numExpectedTableLines, boolean lastItemHasImage)
					throws PartInitException {
				return true;
			}
		};

		final boolean[] done = { false };

		new Thread(new Runnable() {
			public void run() {
				synchronized (done) {
					try {
						test.waitForTableToFill(17, 50, false);
						fail();
					} catch (AssertionFailedError e) {
						done[0] = true;
					} catch (PartInitException e) {
						fail();// bah.
					}
				}
			}
		}).start();

		Thread.sleep(1000);
		synchronized (done) {
			assertTrue(done[0]);
		}
	}

	public void test04stillWaitingChecksForProperNumberOfLines()
			throws Exception {
		WrappingSystemTest test = new WrappingSystemTest() {
			protected int getNumTableItems() throws PartInitException {
				return 2;
			}

			protected synchronized boolean hasNotTerminated() {
				return false;
			}
		};

		assertTrue(test.stillWaiting(17, false));
	}
}
