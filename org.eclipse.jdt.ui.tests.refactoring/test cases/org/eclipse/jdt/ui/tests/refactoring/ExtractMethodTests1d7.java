/*******************************************************************************
 * Copyright (c) 2011, 2021 IBM Corporation and others.
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
public class ExtractMethodTests1d7 extends ExtractMethodTests {

	@Rule
	public ExtractMethodTestSetup1d7 fgTestSetup1d7= new ExtractMethodTestSetup1d7();

	protected void try17Test() throws Exception {
		performTest(fgTestSetup1d7.getTry17Package(), "A", COMPARE_WITH_OUTPUT, "try17_out");
	}

	@Override
	protected void invalidSelectionTest() throws Exception {
		performTest(fgTestSetup1d7.getInvalidSelectionPackage(), "A", INVALID_SELECTION, null);
	}

	//====================================================================================
	// Testing Extracted result
	//====================================================================================

	//---- Test Try / catch block

	@Override
	@Test
	public void test1() throws Exception {
		try17Test();
	}

	@Override
	@Test
	public void test2() throws Exception {
		try17Test();
	}

	@Override
	@Test
	public void test3() throws Exception {
		try17Test();
	}

	@Override
	@Test
	public void test4() throws Exception {
		try17Test();
	}

	@Test
	public void test5() throws Exception {
		try17Test();
	}

	@Test
	public void test6() throws Exception {
		try17Test();
	}

	@Test
	public void test7() throws Exception {
		try17Test();
	}

	@Test
	public void test8() throws Exception {
		try17Test();
	}

	//=====================================================================================
	// Testing invalid selections
	//=====================================================================================

	@Override
	@Test
	public void test010() throws Exception {
		invalidSelectionTest();
	}
}
