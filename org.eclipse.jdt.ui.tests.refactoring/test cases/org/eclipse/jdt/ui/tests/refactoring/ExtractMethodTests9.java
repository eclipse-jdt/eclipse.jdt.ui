/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
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
public class ExtractMethodTests9 extends ExtractMethodTests {

	@Rule
	public ExtractMethodTestSetup9 fgTestSetup9= new ExtractMethodTestSetup9();

	protected void try9Test() throws Exception {
		performTest(fgTestSetup9.getTry9Package(), "A", COMPARE_WITH_OUTPUT, "try9_out");
	}

	@Override
	protected void invalidSelectionTest() throws Exception {
		performTest(fgTestSetup9.getInvalidSelectionPackage(), "A", INVALID_SELECTION, null);
	}

	//====================================================================================
	// Testing invalid selections
	//====================================================================================

	@Override
	@Test
	public void test101() throws Exception {
		invalidSelectionTest();
	}

	//====================================================================================
	// Testing try-with-resources
	//====================================================================================

	@Override
	@Test
	public void test201() throws Exception {
		try9Test();
	}

}
