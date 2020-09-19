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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.eclipse.jdt.ui.tests.CustomBaseRunner;
import org.eclipse.jdt.ui.tests.IgnoreInheritedTests;
import org.eclipse.jdt.ui.tests.refactoring.rules.Java1d7Setup;

/**
 * Those tests should run on Java Dolphin 1.7 .
 */
@IgnoreInheritedTests
@RunWith(CustomBaseRunner.class)
public class IntroduceIndirectionTests1d7 extends IntroduceIndirectionTests {
	public IntroduceIndirectionTests1d7() {
		super(new Java1d7Setup());
	}

	@Test
	public void test17_32() throws Exception {
		// test for bug 349405
		helperPass(new String[] { "p.Foo" }, "foo", "p.Foo", 10, 17, 10, 20);
	}

	@Test
	public void test17_33() throws Exception {
		// test for bug 349405
		helperPass(new String[] { "p.Foo" }, "getX", "p.Foo", 14, 17, 14, 21);
	}

	@Test
	public void test17_34() throws Exception {
		// test for bug
		helperFail(new String[] { "p.Foo" }, "m2", "p.Foo", 7, 18, 7, 18);
	}
}
