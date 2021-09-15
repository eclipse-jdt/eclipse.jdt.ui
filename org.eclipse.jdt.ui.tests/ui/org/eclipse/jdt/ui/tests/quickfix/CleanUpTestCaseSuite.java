/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
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
 *     Suite moved from CleanUpTestCase.java
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	CleanUpStressTest.class,
	CleanUpTest.class,
	CleanUpTest1d4.class,
	CleanUpTest1d5.class,
	CleanUpTest1d6.class,
	CleanUpTest1d7.class,
	CleanUpTest1d8.class,
	CleanUpTest9.class,
	CleanUpTest10.class,
	CleanUpTest11.class,
	CleanUpTest12.class,
	CleanUpTest14.class,
	CleanUpTest15.class,
	CleanUpTest16.class,
	CleanUpAnnotationTest.class,
	SaveParticipantTest.class,
	CleanUpActionTest.class,
	NullAnnotationsCleanUpTest1d8.class
})
public class CleanUpTestCaseSuite {
}
