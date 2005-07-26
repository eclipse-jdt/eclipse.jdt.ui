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
					int millisecondTimeout) throws PartInitException {
				wasCalled[0] = true;
				assertEquals(17, numExpectedTableLines);
			}
		};

		test.runTests(null, 0, 17);
		assertTrue(wasCalled[0]);
	}

	public void test02waitForTableToFillWaitsForNumberOfLines()
			throws Exception {
		WrappingSystemTest test = new WrappingSystemTest() {
			protected boolean stillWaiting(int numExpectedTableLines)
					throws PartInitException {
				assertEquals(17, numExpectedTableLines);
				return false;
			}
		};
		test.waitForTableToFill(17, 30000);
	}

	public void test03waitForTableToFillObeysTimeout() throws Exception {
		final WrappingSystemTest test = new WrappingSystemTest() {
			protected void dispatchEvents() {
				// do nothing
			}

			protected boolean stillWaiting(int numExpectedTableLines)
					throws PartInitException {
				return true;
			}
		};

		final boolean[] done = { false };

		new Thread(new Runnable() {
			public void run() {
				try {
					test.waitForTableToFill(17, 50);
				} catch (AssertionFailedError e) {
					synchronized (done) {
						done[0] = true;
					}
				} catch (PartInitException e) {
					// bah.
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
		
		assertTrue(test.stillWaiting(17));
	}
}
