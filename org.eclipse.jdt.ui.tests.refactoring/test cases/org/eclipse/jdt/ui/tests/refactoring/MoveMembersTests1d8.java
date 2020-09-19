/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
public class MoveMembersTests1d8 extends MoveMembersTests {
	public MoveMembersTests1d8() {
		super(new Java1d8Setup());
	}

	@Test
	public void test18_1() throws Exception {
		// move private static method from class to interface, update visibility
		methodHelper_passing(new String[] { "m" }, new String[][] { new String[0] });
	}

	@Test
	public void test18_2() throws Exception {
		// move static method from interface to another interface
		methodHelper_passing(new String[] { "m" }, new String[][] { new String[0] });
	}

	@Test
	public void test18_3() throws Exception {
		// move static method from interface to class, add 'public'
		methodHelper_passing(new String[] { "m" }, new String[][] { new String[0] });
	}
}
