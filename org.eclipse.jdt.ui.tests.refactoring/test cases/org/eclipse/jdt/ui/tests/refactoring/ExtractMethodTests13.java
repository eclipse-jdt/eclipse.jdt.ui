/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
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
import static org.eclipse.jdt.ui.tests.refactoring.AbstractJunit4SelectionTestCase.TestMode.INVALID_SELECTION;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.eclipse.jdt.ui.tests.CustomBaseRunner;
import org.eclipse.jdt.ui.tests.IgnoreInheritedTests;

@IgnoreInheritedTests
@RunWith(CustomBaseRunner.class)
public class ExtractMethodTests13 extends ExtractMethodTests {

	@SuppressWarnings("hiding")
	@Rule
	public ExtractMethodTestSetup13 fgTestSetup= new ExtractMethodTestSetup13();

	@Override
	protected void invalidSelectionTest() throws Exception {
		performTest(fgTestSetup.getInvalidSelectionPackage(), "A", INVALID_SELECTION, null);
	}

	protected void try13Test() throws Exception {
		performTest(fgTestSetup.getTry13Package(), "A", COMPARE_WITH_OUTPUT, "try13_out");
	}

	@Test
	public void testSwitchExpr1() throws Exception {
		try13Test();
	}

	@Test
	public void testSwitchExpr2() throws Exception {
		invalidSelectionTest();
	}

}
