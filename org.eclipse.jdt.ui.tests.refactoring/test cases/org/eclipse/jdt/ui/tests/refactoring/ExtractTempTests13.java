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

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.eclipse.jdt.ui.tests.CustomBaseRunner;
import org.eclipse.jdt.ui.tests.IgnoreInheritedTests;
import org.eclipse.jdt.ui.tests.refactoring.rules.Java13Setup;

@IgnoreInheritedTests
@RunWith(CustomBaseRunner.class)
public class ExtractTempTests13 extends ExtractTempTests {
	@Rule
	public Java13Setup js= new Java13Setup();

	@Override
	protected String getTestFileName(boolean canExtract, boolean input) {
		StringBuilder fileName= new StringBuilder(TEST_PATH_PREFIX).append(getRefactoringPath());
		fileName.append(canExtract ? "canExtract13/" : "cannotExtract13/");
		return fileName.append(getSimpleTestFileName(canExtract, input)).toString();
	}

	//--- TESTS

	@Test
	public void test120() throws Exception {
		helper1(6, 19, 9, 22, true, false, "string", "string");
	}
}
