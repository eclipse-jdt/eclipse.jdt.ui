/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests.performance;

import junit.framework.Test;
import junit.framework.TestSuite;


/**
 * @since 3.1
 */
public class AllPerformanceTestSuite extends TestSuite {

	private static final boolean RUN_DEBUGGING_TEST_SUITE= false;

	public static Test suite() {
		return new AllPerformanceTestSuite();
	}

	public AllPerformanceTestSuite() {
		// The Debug performance tests must be run separately in debug mode
		if (RUN_DEBUGGING_TEST_SUITE) {
			addTest(PerformanceTestSuite.suite());
			addTest(PerformanceTestSuite2.suite());
			addTest(PerformanceTestSuite3.suite());
			addTest(PerformanceTestSuite4.suite());
			addTest(PerformanceTestSuite5.suite());
			addTest(EventDrivenTestSuite.suite());
		} else
			addTest(getDebuggingPerformanceTestSuite());
	}

	/**
	 * @return the test suite
	 * @deprecated since INVOCATION_COUNT dimension is no longer supported.
	 */
	private Test getDebuggingPerformanceTestSuite() {
		return DebuggingPerformanceTestSuite.suite();
	}
}
