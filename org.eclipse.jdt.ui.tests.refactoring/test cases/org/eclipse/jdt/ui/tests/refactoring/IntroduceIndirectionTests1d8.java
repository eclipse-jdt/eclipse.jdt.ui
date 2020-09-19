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
public class IntroduceIndirectionTests1d8 extends IntroduceIndirectionTests {
	public IntroduceIndirectionTests1d8() {
		super(new Java1d8Setup());
	}

	@Test
	public void test18_01() throws Exception {
		helperPass(new String[] { "p.Foo" }, "d", "p.Foo", 5, 17, 5, 18);
	}

	@Test
	public void test18_02() throws Exception {
		helperErr(new String[] { "p.Foo" }, "s", "p.Foo", 5, 16, 5, 17);
	}

	@Test
	public void test18_03() throws Exception {
		helperPass(new String[] { "p.Foo" }, "a", "p.Foo", 4, 9, 4, 10);
	}

	@Test
	public void test18_04() throws Exception {
		helperPass(new String[] { "p.C", "p.Foo" }, "d", "p.Foo", 4, 9, 4, 10);
	}

	@Test
	public void test18_05() throws Exception {
		helperPass(new String[] { "p.C", "p.Foo" }, "s", "p.Foo", 4, 16, 4, 17);
	}

	@Test
	public void test18_06() throws Exception {
		helperPass(new String[] { "p.Foo", "p.C" }, "d", "p.C", 5, 17, 5, 18);
	}
}
