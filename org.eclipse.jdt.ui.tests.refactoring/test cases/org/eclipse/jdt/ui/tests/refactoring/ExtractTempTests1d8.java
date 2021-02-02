/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
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

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.ui.tests.CustomBaseRunner;
import org.eclipse.jdt.ui.tests.IgnoreInheritedTests;
import org.eclipse.jdt.ui.tests.refactoring.rules.Java1d8Setup;

/**
 * Those tests are made to run on Java Spider 1.8 .
 */
@IgnoreInheritedTests
@RunWith(CustomBaseRunner.class)
public class ExtractTempTests1d8 extends ExtractTempTests {

	public ExtractTempTests1d8() {
		super(new Java1d8Setup());
	}

	@Override
	protected String getTestFileName(boolean canExtract, boolean input) {
		StringBuilder fileName= new StringBuilder(TEST_PATH_PREFIX).append(getRefactoringPath());
		fileName.append(canExtract ? "canExtract18/" : "cannotExtract18/");
		return fileName.append(getSimpleTestFileName(canExtract, input)).toString();
	}

	//--- TESTS

	@Override
	@Test
	public void test110() throws Exception {
		helper1(6, 77, 6, 82, true, false, "x", "x");
	}

	@Override
	@Test
	public void test111() throws Exception {
		helper1(6, 73, 6, 83, true, false, "foo", "foo");
	}

	@Test
	public void test112() throws Exception {
		helper1(6, 51, 6, 57, true, false, "x2", "i");
	}

	@Override
	@Test
	public void test113() throws Exception {
		helper1(7, 28, 7, 34, true, false, "x2", "i");
	}

	@Override
	@Test
	public void test114() throws Exception {
		helper1(6, 32, 6, 44, true, false, "string", "string");
	}

	@Override
	@Test
	public void test115() throws Exception {
		helper1(6, 32, 6, 32, true, false, "x2", "x2");
	}

	@Test
	public void test116() throws Exception {
		helper1(6, 44, 6, 44, true, false, "string", "string");
	}

	@Test
	public void test117() throws Exception {
		helper1(6, 32, 6, 32, true, false, "integer", "integer");
	}

	@Test
	public void test118() throws Exception {
		helper1(6, 59, 6, 59, true, false, "string", "string");
	}

	@Test
	public void test119() throws Exception {
		helper1(7, 30, 7, 63, true, false, "supplier", "supplier");
	}

	// -- testing failing preconditions
	@Override
	@Test
	public void testFail1() throws Exception {
		failHelper1(6, 32, 6, 58, false, false, "temp", RefactoringStatus.FATAL);
	}

	@Override
	@Test
	public void testFail2() throws Exception {
		failHelper1(6, 27, 6, 58, false, false, "temp", RefactoringStatus.FATAL);
	}
}
