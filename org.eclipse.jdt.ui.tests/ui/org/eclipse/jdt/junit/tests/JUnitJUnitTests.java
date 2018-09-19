/*******************************************************************************
 * Copyright (c) 2005, 2017 IBM Corporation and others.
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

import junit.framework.Test;
import junit.framework.TestSuite;

public class JUnitJUnitTests {

	public static Test suite() {
		TestSuite suite= new TestSuite(JUnitJUnitTests.class.getName());
		//$JUnit-BEGIN$

		// TODO disabled unreliable tests driving the event loop:
//		suite.addTestSuite(WrappingSystemTest.class);
//		suite.addTestSuite(WrappingUnitTest.class);

		suite.addTestSuite(TestEnableAssertions.class);
		suite.addTestSuite(TestPriorization.class);
		suite.addTestSuite(TestTestSearchEngine.class);

		addDeprecatedTests(suite);

		suite.addTestSuite(TestRunListenerTest3.class);
		suite.addTestSuite(TestRunListenerTest4.class);
		suite.addTestSuite(TestRunListenerTest5.class);
		
		suite.addTestSuite(TestRunFilteredStandardRunnerTest4.class);
		suite.addTestSuite(TestRunFilteredParameterizedRunnerTest4.class);

		suite.addTest(TestRunSessionSerializationTests3.suite());
		suite.addTest(TestRunSessionSerializationTests4.suite());

		suite.addTestSuite(JUnit3TestFinderTest.class);
		suite.addTestSuite(JUnit4TestFinderTest.class);
		//$JUnit-END$
		return suite;
	}

	/**
	 * @param suite the suite
	 * @deprecated to hide deprecation warning
	 */
	@Deprecated
	private static void addDeprecatedTests(TestSuite suite) {
		suite.addTestSuite(LegacyTestRunListenerTest.class);
	}

}
