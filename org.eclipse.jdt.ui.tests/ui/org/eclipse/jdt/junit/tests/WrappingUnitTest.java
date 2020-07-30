/*******************************************************************************
 * Copyright (c) 2005, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.junit.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Ignore;
import org.junit.Test;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.ui.PartInitException;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.junit.ui.TextualTrace;

/**
 * Disabled unreliable tests driving the event loop in JUnitJUnitTests.
 */
public class WrappingUnitTest {
	@Test
	public void test00wrapSecondLine() throws Exception {
		TextualTrace trace = new TextualTrace("12345\n1234512345",
				new String[0]);
		trace.display((lineType, label) -> assertEquals("12345", label), 5);
	}

	@Test
	public void test01wrappingSystemTestRunTestsWaitsForCorrectNumberOfLines()
			throws Exception {
		final boolean[] wasCalled = { false };
		WrappingSystemTest test = new WrappingSystemTest() {
			@Override
			protected void launchTests(String prefixForErrorMessage,
					int howManyNumbersInErrorString) throws CoreException,
					JavaModelException {
				// do nothing
			}

			@Override
			protected void waitForTableToFill(int numExpectedTableLines,
					int millisecondTimeout, boolean lastItemHasImage) throws PartInitException {
				wasCalled[0] = true;
				assertEquals(17, numExpectedTableLines);
			}
		};

		test.runTests(null, 0, 17, false);
		assertTrue(wasCalled[0]);
	}

	@Test
	public void test02waitForTableToFillWaitsForNumberOfLines()
			throws Exception {
		WrappingSystemTest test = new WrappingSystemTest() {
			@Override
			protected boolean stillWaiting(int numExpectedTableLines, boolean lastItemHasImage)
					throws PartInitException {
				assertEquals(17, numExpectedTableLines);
				return false;
			}
		};
		test.waitForTableToFill(17, 30000, false);
	}

	@Ignore("java.lang.AssertionError")
	@Test
	public void test03waitForTableToFillObeysTimeout() throws Exception {
		final WrappingSystemTest test = new WrappingSystemTest() {
			@Override
			protected void dispatchEvents() {
				// do nothing (avoid accessing display from non-UI thread)
			}

			@Override
			protected int getNumTableItems() throws PartInitException {
				return -1; // avoid accessing getActiveWorkbenchWindow() from non-UI thread
			}

			@Override
			protected boolean stillWaiting(int numExpectedTableLines, boolean lastItemHasImage)
					throws PartInitException {
				return true;
			}
		};

		final boolean[] done = { false };

		new Thread(() -> {
			synchronized (done) {
				try {
					test.waitForTableToFill(17, 50, false);
					fail();
//					} catch (AssertionFailedError e) {
//						done[0] = true;
				} catch (PartInitException e) {
					fail();// bah.
				}
			}
		}).start();

		Thread.sleep(1000);
		synchronized (done) {
			assertTrue(done[0]);
		}
	}

	@Test
	public void test04stillWaitingChecksForProperNumberOfLines()
			throws Exception {
		WrappingSystemTest test = new WrappingSystemTest() {
			@Override
			protected int getNumTableItems() throws PartInitException {
				return 2;
			}

			@Override
			protected synchronized boolean hasNotTerminated() {
				return false;
			}
		};

		assertTrue(test.stillWaiting(17, false));
	}
}
