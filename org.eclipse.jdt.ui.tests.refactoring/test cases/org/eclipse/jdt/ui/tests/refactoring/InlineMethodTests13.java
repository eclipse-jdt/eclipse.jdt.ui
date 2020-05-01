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

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.eclipse.jdt.ui.tests.CustomBaseRunner;
import org.eclipse.jdt.ui.tests.IgnoreInheritedTests;

@IgnoreInheritedTests
@RunWith(CustomBaseRunner.class)
public class InlineMethodTests13 extends InlineMethodTests {

	@Rule
	public InlineMethodTestSetup13 fgTestSetup= new InlineMethodTestSetup13();

	protected void performSimpleTest() throws Exception {
		performTestInlineCall(fgTestSetup.getSimplePackage(), getName(), COMPARE_WITH_OUTPUT, "simple13_out");
	}

	//--- TESTS

	@Test
	public void testTextBlock1() throws Exception {
		performSimpleTest();
	}
}
