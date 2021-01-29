/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
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

/**
 * Those tests are made to run on Java Spider 1.8 .
 */
@IgnoreInheritedTests
@RunWith(CustomBaseRunner.class)
public class InlineMethodTests1d8 extends InlineMethodTests {
	@Rule
	public InlineMethodTestSetup1d8 fgTestSetup1d8= new InlineMethodTestSetup1d8();

	protected void performSimpleTest() throws Exception {
		performTestInlineCall(fgTestSetup1d8.getSimplePackage(), getName(), COMPARE_WITH_OUTPUT, "simple18_out");
	}

	//--- TESTS

	@Test
	public void testLambda1() throws Exception {
		performSimpleTest();
	}
}
