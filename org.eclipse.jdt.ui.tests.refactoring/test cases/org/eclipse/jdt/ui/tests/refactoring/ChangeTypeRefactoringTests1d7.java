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

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.eclipse.jdt.testplugin.StringAsserts;

import org.eclipse.jdt.ui.tests.CustomBaseRunner;
import org.eclipse.jdt.ui.tests.IgnoreInheritedTests;
import org.eclipse.jdt.ui.tests.refactoring.rules.Java1d7Setup;

/**
 * Those tests should run on Java Dolphin 1.7 .
 */
@IgnoreInheritedTests
@RunWith(CustomBaseRunner.class)
public class ChangeTypeRefactoringTests1d7 extends ChangeTypeRefactoringTests {

	public ChangeTypeRefactoringTests1d7() {
		rts= new Java1d7Setup();
	}
	@Override
	protected String getTestFileName(boolean positive, boolean input) {
		StringBuilder fileName= new StringBuilder(TEST_PATH_PREFIX).append(getRefactoringPath());

		fileName.append(positive ? "positive17/" : "negative17/");
		fileName.append(getSimpleTestFileName(input));
		return fileName.toString();
	}

	//--- TESTS
	@Test
	public void testTryWithResources() throws Exception {
		Collection<String> types= helper1(7, 25, 7, 31, "java.io.InputStreamReader").getValidTypeNames();
		String[] actual= types.toArray(new String[types.size()]);
		String[] expected= { "java.io.InputStreamReader", "java.io.Reader" };
		StringAsserts.assertEqualStringsIgnoreOrder(actual, expected);
	}

	// tests that are supposed to fail

	@Test
	public void testUnionType() throws Exception {
		failHelper1(12, 65, 12, 67, 4, "java.lang.Object");
	}
}
