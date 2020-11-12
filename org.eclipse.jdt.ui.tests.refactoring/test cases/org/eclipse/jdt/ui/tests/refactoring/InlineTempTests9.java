/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
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

import org.eclipse.jdt.ui.tests.CustomBaseRunner;
import org.eclipse.jdt.ui.tests.IgnoreInheritedTests;
import org.eclipse.jdt.ui.tests.refactoring.rules.Java9Setup;
import org.eclipse.jdt.ui.tests.refactoring.rules.RefactoringTestSetup;

@IgnoreInheritedTests
@RunWith(CustomBaseRunner.class)
public class InlineTempTests9 extends InlineTempTests {
	@Rule
	public RefactoringTestSetup js= new Java9Setup();

	@Override
	protected String getTestFileName(boolean canInline, boolean input) {
		StringBuilder fileName= new StringBuilder(TEST_PATH_PREFIX).append(getRefactoringPath());
		fileName.append(canInline ? "canInline9/" : "cannotInline9/");
		return fileName.append(getSimpleTestFileName(canInline, input)).toString();
	}

	//--- tests for try-with-resources

	@Override
	@Test
	public void test0() throws Exception {
		helper2(9, 19, 9, 28);
	}

	@Override
	@Test
	public void test1() throws Exception {
		helper2(10, 37, 10, 46);
	}
}
