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

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.eclipse.jdt.ui.tests.CustomBaseRunner;
import org.eclipse.jdt.ui.tests.IgnoreInheritedTests;
import org.eclipse.jdt.ui.tests.refactoring.rules.Java13Setup;
import org.eclipse.jdt.ui.tests.refactoring.rules.RefactoringTestSetup;

@IgnoreInheritedTests
@RunWith(CustomBaseRunner.class)
public class ExtractConstantTests13 extends ExtractConstantTests {
	@Rule
	public RefactoringTestSetup rts= new Java13Setup();

	@Override
	protected String getTestFileName(boolean canExtract, boolean input) {
		String fileName= TEST_PATH_PREFIX + getRefactoringPath();
		fileName+= (canExtract ? "canExtract13/" : "cannotExtract13/");
		return fileName + getSimpleTestFileName(canExtract, input);
	}

	//--- TESTS

	@Override
	@Test
	public void test0() throws Exception {
		helper1(6, 19, 9, 22, true, false, "CONSTANT", "STRING");
	}
}
