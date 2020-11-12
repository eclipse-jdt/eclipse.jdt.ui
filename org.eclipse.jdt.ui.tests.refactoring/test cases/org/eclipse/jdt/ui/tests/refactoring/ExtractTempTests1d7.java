/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.ui.tests.CustomBaseRunner;
import org.eclipse.jdt.ui.tests.IgnoreInheritedTests;
import org.eclipse.jdt.ui.tests.refactoring.rules.Java1d7Setup;
import org.eclipse.jdt.ui.tests.refactoring.rules.RefactoringTestSetup;

/**
 * Those tests should run on Java Dolphin 1.7 .
 */
@IgnoreInheritedTests
@RunWith(CustomBaseRunner.class)
public class ExtractTempTests1d7 extends ExtractTempTests {
	@Rule
	public RefactoringTestSetup js= new Java1d7Setup();

	@Override
	protected String getTestFileName(boolean canExtract, boolean input){
		StringBuilder fileName= new StringBuilder(TEST_PATH_PREFIX).append(getRefactoringPath());
		fileName.append(canExtract ? "canExtract17/" : "cannotExtract17/");
		return fileName.append(getSimpleTestFileName(canExtract, input)).toString();
	}

	//--- TESTS

	@Override
	@Test
	public void test110() throws Exception {
		helper1(14, 13, 14, 15, true, false, "temp", "ex2");
	}

	@Override
	@Test
	public void test111() throws Exception {
		helper1(8, 16, 8, 33, true, false, "arrayList", "arrayList");
	}

	@Test
	public void test112() throws Exception {
		helper1(8, 20, 8, 37, true, false, "arrayList", "arrayList");
	}

	@Override
	@Test
	public void test113() throws Exception {
		helper1(12, 16, 12, 33, true, false, "arrayList2", "arrayList2");
	}

	@Override
	@Test
	public void test114() throws Exception {
		helper1(9, 34, 9, 56, true, false, "fileReader", "fileReader");
	}

	// -- testing failing preconditions
	@Override
	@Test
	public void testFail1() throws Exception {
		failHelper1(9, 14, 9, 56, false, false, "temp", RefactoringStatus.FATAL);
	}
}
