/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
import org.eclipse.jdt.ui.tests.refactoring.rules.Java16Setup;

@IgnoreInheritedTests
@RunWith(CustomBaseRunner.class)
public class ExtractTempTests16 extends ExtractTempTests {

	public ExtractTempTests16() {
		super(new Java16Setup());
	}

	@Override
	protected String getTestFileName(boolean canExtract, boolean input) {
		StringBuilder fileName= new StringBuilder(TEST_PATH_PREFIX).append(getRefactoringPath());
		fileName.append(canExtract ? "canExtract16/" : "cannotExtract16/");
		return fileName.append(getSimpleTestFileName(canExtract, input)).toString();
	}

	//--- TESTS

	@Override
	@Test
	public void test160() throws Exception {
		helper1(10, 24, 10, 36, true, false, "token", "token");
	}

	@Override
	@Test
	public void test161() throws Exception {
		helper1(10, 24, 10, 36, false, false, "token", "token");
	}
}
