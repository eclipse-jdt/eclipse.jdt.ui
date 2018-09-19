/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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

import junit.framework.Test;

public class IntroduceIndirectionTests18 extends IntroduceIndirectionTests{
	private static final Class<IntroduceIndirectionTests18> clazz= IntroduceIndirectionTests18.class;

	public IntroduceIndirectionTests18(String name) {
		super(name);
	}

	public static Test setUpTest(Test test) {
		return new Java18Setup(test);
	}

	public static Test suite() {
		return setUpTest(new NoSuperTestsSuite(clazz));
	}

// ---

	public void test18_01() throws Exception {
		helperPass(new String[] { "p.Foo" }, "d", "p.Foo", 5, 17, 5, 18);
	}

	public void test18_02() throws Exception {
		helperErr(new String[] { "p.Foo" }, "s", "p.Foo", 5, 16, 5, 17);
	}

	public void test18_03() throws Exception {
		helperPass(new String[] { "p.Foo" }, "a", "p.Foo", 4, 9, 4, 10);
	}

	public void test18_04() throws Exception {
		helperPass(new String[] { "p.C", "p.Foo" }, "d", "p.Foo", 4, 9, 4, 10);
	}

	public void test18_05() throws Exception {
		helperPass(new String[] { "p.C", "p.Foo" }, "s", "p.Foo", 4, 16, 4, 17);
	}

	public void test18_06() throws Exception {
		helperPass(new String[] { "p.Foo", "p.C" }, "d", "p.C", 5, 17, 5, 18);
	}
}
