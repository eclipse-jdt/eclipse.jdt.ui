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
 *     Sandra Lions <sandra.lions-piron@oracle.com> - [JUnit] allow to sort by name and by execution time - https://bugs.eclipse.org/bugs/show_bug.cgi?id=219466
 *******************************************************************************/

package org.eclipse.jdt.junit.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
// TODO disabled unreliable tests driving the event loop:
//WrappingSystemTest.class,
//WrappingUnitTest.class,

TestEnableAssertions.class,
TestPriorization.class,
TestTestSearchEngine.class,

TestRunListenerTest3.class,
TestRunListenerTest4.class,
TestRunListenerTest5.class,

TestRunFilteredStandardRunnerTest4.class,
TestRunFilteredParameterizedRunnerTest4.class,

TestRunSessionSerializationTests3.class,
TestRunSessionSerializationTests4.class,

JUnit3TestFinderTest.class,
JUnit4TestFinderTest.class,

TestSorting.class
/**
 * @param suite the suite
 * @deprecated to hide deprecation warning
 */
//LegacyTestRunListenerTest.class
})
public class JUnitJUnitTests {

}
