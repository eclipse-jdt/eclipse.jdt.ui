/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.eclipse.jdt.ui.tests.CustomBaseRunner;
import org.eclipse.jdt.ui.tests.IgnoreInheritedTests;
import org.eclipse.jdt.ui.tests.refactoring.rules.Java1d8Setup;

/**
 * Those tests are made to run on Java Spider 1.8 .
 */
@IgnoreInheritedTests
@RunWith(CustomBaseRunner.class)
public class InlineTempTests1d8 extends InlineTempTests {

	public InlineTempTests1d8() {
		super(new Java1d8Setup());
	}

	@Override
	protected String getTestFileName(boolean canInline, boolean input) {
		StringBuilder fileName= new StringBuilder(TEST_PATH_PREFIX).append(getRefactoringPath());
		fileName.append(canInline ? "canInline18/" : "cannotInline18/");
		return fileName.append(getSimpleTestFileName(canInline, input)).toString();
	}

	//--- tests for lambda expressions

	@Override
	@Test
	public void test0() throws Exception {
		helper1(6, 18, 6, 20);
	}

	@Override
	@Test
	public void test1() throws Exception {
		helper1(6, 18, 6, 20);
	}

	@Override
	@Test
	public void test2() throws Exception {
		helper1(5, 20, 5, 25);
	}

	@Override
	@Test
	public void test3() throws Exception {
		helper1(9, 29, 9, 36);
	}

	//--- tests for method references

	@Test
	public void test1000() throws Exception {
		helper1(6, 18, 6, 20);
	}

	@Test
	public void test1001() throws Exception {
		helper1(6, 18, 6, 20);
	}
}
