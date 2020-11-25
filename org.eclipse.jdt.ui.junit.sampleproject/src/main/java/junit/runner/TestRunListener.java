package junit.runner;

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

/**
 * A listener interface for observing the execution of a test run. Unlike
 * TestListener, this interface using only primitive objects, making it suitable
 * for remote test execution.
 */
public interface TestRunListener {
	/* test status constants */
	int STATUS_ERROR = 1;
	int STATUS_FAILURE = 2;

	void testRunStarted(String testSuiteName, int testCount);

	void testRunEnded(long elapsedTime);

	void testRunStopped(long elapsedTime);

	void testStarted(String testName);

	void testEnded(String testName);

	void testFailed(int status, String testName, String trace);
}
