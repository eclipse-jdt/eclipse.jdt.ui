/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
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

public class MoveMembersTests18 extends MoveMembersTests {

	private static final Class<MoveMembersTests18> clazz= MoveMembersTests18.class;

	public MoveMembersTests18(String name) {
		super(name);
	}

	public static Test suite() {
		return setUpTest(new NoSuperTestsSuite(clazz));
	}

	public static Test setUpTest(Test someTest) {
		return new Java18Setup(someTest);
	}

	public void test18_1() throws Exception {
		// move private static method from class to interface, update visibility
		methodHelper_passing(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void test18_2() throws Exception {
		// move static method from interface to another interface
		methodHelper_passing(new String[] { "m" }, new String[][] { new String[0] });
	}

	public void test18_3() throws Exception {
		// move static method from interface to class, add 'public'
		methodHelper_passing(new String[] { "m" }, new String[][] { new String[0] });
	}
}
