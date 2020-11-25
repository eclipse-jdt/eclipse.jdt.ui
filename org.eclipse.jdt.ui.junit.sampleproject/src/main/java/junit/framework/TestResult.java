package junit.framework;

/*-
 * #%L
 * org.eclipse.jdt.ui.junit.sampleproject
 * %%
 * Copyright (C) 2020 Eclipse Foundation
 * %%
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 * #L%
 */

import java.util.Vector;
import java.util.Enumeration;

/**
 * A <code>TestResult</code> collects the results of executing a test case. It
 * is an instance of the Collecting Parameter pattern. The test framework
 * distinguishes between <i>failures</i> and <i>errors</i>. A failure is
 * anticipated and checked for with assertions. Errors are unanticipated
 * problems like an <code>ArrayIndexOutOfBoundsException</code>.
 *
 * @see Test
 */
public class TestResult extends Object {
	protected Vector<TestFailure> fFailures;
	protected Vector<TestFailure> fErrors;
	protected Vector<TestListener> fListeners;
	protected int fRunTests;
	private boolean fStop;

	public TestResult() {
		fFailures = new Vector<TestFailure>();
		fErrors = new Vector<TestFailure>();
		fListeners = new Vector<TestListener>();
		fRunTests = 0;
		fStop = false;
	}

	/**
	 * Adds an error to the list of errors. The passed in exception caused the
	 * error.
	 */
	public synchronized void addError(Test test, Throwable t) {
		fErrors.addElement(new TestFailure(test, t));
		for (Object element : cloneListeners()) {
			((TestListener) element).addError(test, t);
		}
	}

	/**
	 * Adds a failure to the list of failures. The passed in exception caused the
	 * failure.
	 */
	public synchronized void addFailure(Test test, AssertionFailedError t) {
		fFailures.addElement(new TestFailure(test, t));
		for (Object element : cloneListeners()) {
			((TestListener) element).addFailure(test, t);
		}
	}

	/**
	 * Registers a TestListener
	 */
	public synchronized void addListener(TestListener listener) {
		fListeners.addElement(listener);
	}

	/**
	 * Unregisters a TestListener
	 */
	public synchronized void removeListener(TestListener listener) {
		fListeners.removeElement(listener);
	}

	/**
	 * Returns a copy of the listeners.
	 */
	private synchronized Vector<TestListener> cloneListeners() {
		return (Vector<TestListener>) fListeners.clone();
	}

	/**
	 * Informs the result that a test was completed.
	 */
	public void endTest(Test test) {
		for (Object element : cloneListeners()) {
			((TestListener) element).endTest(test);
		}
	}

	/**
	 * Gets the number of detected errors.
	 */
	public synchronized int errorCount() {
		return fErrors.size();
	}

	/**
	 * Returns an Enumeration for the errors
	 */
	public synchronized Enumeration<TestFailure> errors() {
		return fErrors.elements();
	}

	/**
	 * Gets the number of detected failures.
	 */
	public synchronized int failureCount() {
		return fFailures.size();
	}

	/**
	 * Returns an Enumeration for the failures
	 */
	public synchronized Enumeration<TestFailure> failures() {
		return fFailures.elements();
	}

	/**
	 * Runs a TestCase.
	 */
	protected void run(final TestCase test) {
		startTest(test);
		Protectable p = new Protectable() {
			public void protect() throws Throwable {
				test.runBare();
			}
		};
		runProtected(test, p);

		endTest(test);
	}

	/**
	 * Gets the number of run tests.
	 */
	public synchronized int runCount() {
		return fRunTests;
	}

	/**
	 * Runs a TestCase.
	 */
	public void runProtected(final Test test, Protectable p) {
		try {
			p.protect();
		} catch (AssertionFailedError e) {
			addFailure(test, e);
		} catch (ThreadDeath e) { // don't catch ThreadDeath by accident
			throw e;
		} catch (Throwable e) {
			addError(test, e);
		}
	}

	/**
	 * Checks whether the test run should stop
	 */
	public synchronized boolean shouldStop() {
		return fStop;
	}

	/**
	 * Informs the result that a test will be started.
	 */
	public void startTest(Test test) {
		final int count = test.countTestCases();
		synchronized (this) {
			fRunTests += count;
		}
		for (Object element : cloneListeners()) {
			((TestListener) element).startTest(test);
		}
	}

	/**
	 * Marks that the test run should stop.
	 */
	public synchronized void stop() {
		fStop = true;
	}

	/**
	 * Returns whether the entire test was successful or not.
	 */
	public synchronized boolean wasSuccessful() {
		return failureCount() == 0 && errorCount() == 0;
	}
}
