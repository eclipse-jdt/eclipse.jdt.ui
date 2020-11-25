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

/**
 * A <em>Test</em> can be run and collect its results.
 *
 * @see TestResult
 */
public interface Test {
	/**
	 * Counts the number of test cases that will be run by this test.
	 */
	int countTestCases();

	/**
	 * Runs a test and collects its result in a TestResult instance.
	 */
	void run(TestResult result);
}
