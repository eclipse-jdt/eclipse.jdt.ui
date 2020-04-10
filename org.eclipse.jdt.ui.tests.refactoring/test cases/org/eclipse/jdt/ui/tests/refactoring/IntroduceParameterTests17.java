/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
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
import org.eclipse.jdt.ui.tests.refactoring.rules.Java17Setup;
import org.eclipse.jdt.ui.tests.refactoring.rules.RefactoringTestSetup;

@IgnoreInheritedTests
@RunWith(CustomBaseRunner.class)
public class IntroduceParameterTests17 extends IntroduceParameterTests {
	@Rule
	public RefactoringTestSetup rts= new Java17Setup();

// ---

	@Test
	public void testSimple17_Catch1() throws Exception {
		performOK();
	}

	@Test
	public void testSimple17_Catch2() throws Exception {
		performOK();
	}

	@Test
	public void testSimple17_NewInstance2() throws Exception {
		performOK();
	}

	@Test
	public void testSimple17_NewInstance3() throws Exception {
		performOK();
	}

	@Test
	public void testSimple17_NewInstance4() throws Exception {
		performOK();
	}
}
