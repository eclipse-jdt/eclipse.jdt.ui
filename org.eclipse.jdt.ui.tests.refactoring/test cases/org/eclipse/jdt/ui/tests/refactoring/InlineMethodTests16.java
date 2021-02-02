/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
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

import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.ui.tests.CustomBaseRunner;
import org.eclipse.jdt.ui.tests.IgnoreInheritedTests;

@IgnoreInheritedTests
@RunWith(CustomBaseRunner.class)
public class InlineMethodTests16 extends InlineMethodTests {

	@Rule
	public InlineMethodTestSetup16 fgSetup= new InlineMethodTestSetup16();

	protected void performSimpleTest() throws Exception {
		performTestInlineCall(fgSetup.getSimplePackage(), getName(), COMPARE_WITH_OUTPUT, fgSetup.getSimplePkgOutName());
	}

	protected void performDefaultTest() throws Exception {
		performTestInlineCall(fgSetup.getDefaultPackage(), getName(), COMPARE_WITH_OUTPUT, fgSetup.getSimplePkgOutName());
	}

	@Override
	protected String getFilePath(IPackageFragment pack, String name) {
		IPackageFragment packToUse = pack;
		if (pack == fgSetup.getDefaultPackage()) {
			packToUse= fgSetup.getSimplePackage();
		}
		return super.getFilePath(packToUse, name);
	}

	//--- TESTS

	@Test
	public void testTextBlock1() throws Exception {
		performSimpleTest();
	}

	@Test
	public void testInlineProblem1() throws Exception {
		performDefaultTest();
	}

	@Test
	public void testInlineProblem2() throws Exception {
		performDefaultTest();
	}
}
