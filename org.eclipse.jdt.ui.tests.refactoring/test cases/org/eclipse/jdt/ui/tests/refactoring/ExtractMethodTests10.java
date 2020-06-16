/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.refactoring;

import static org.eclipse.jdt.ui.tests.refactoring.AbstractJunit4SelectionTestCase.TestMode.COMPARE_WITH_OUTPUT;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.eclipse.jdt.ui.tests.CustomBaseRunner;
import org.eclipse.jdt.ui.tests.IgnoreInheritedTests;

@IgnoreInheritedTests
@RunWith(CustomBaseRunner.class)
public class ExtractMethodTests10 extends ExtractMethodTests {

	@SuppressWarnings("hiding")
	@Rule
	public ExtractMethodTestSetup10 fgTestSetup= new ExtractMethodTestSetup10();

	protected void try10Test() throws Exception {
		performTest(fgTestSetup.getTry10Package(), "A", COMPARE_WITH_OUTPUT, "try10_out");
	}

	//====================================================================================
	// Testing var type
	//====================================================================================

	@Test
	public void testVar1() throws Exception {
		try10Test();
	}

}
