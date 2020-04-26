/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
import org.eclipse.jdt.ui.tests.refactoring.rules.Java1d7Setup;
import org.eclipse.jdt.ui.tests.refactoring.rules.RefactoringTestSetup;

/**
 * Those tests should run on Java Dolphin 1.7 .
 */
@IgnoreInheritedTests
@RunWith(CustomBaseRunner.class)
public class ExtractConstantTests1d7 extends ExtractConstantTests {
	@Rule
	public RefactoringTestSetup rts= new Java1d7Setup();

	@Override
	protected String getTestFileName(boolean canExtract, boolean input) {
		String fileName= TEST_PATH_PREFIX + getRefactoringPath();
		fileName+= (canExtract ? "canExtract17/" : "cannotExtract17/");
		return fileName + getSimpleTestFileName(canExtract, input);
	}

	//--- TESTS

	// -- testing failing preconditions
	@Override
	@Test
	public void testFail0() throws Exception{
		failHelper1(10, 14, 10, 56, true, true, "CONSTANT");
	}
}
