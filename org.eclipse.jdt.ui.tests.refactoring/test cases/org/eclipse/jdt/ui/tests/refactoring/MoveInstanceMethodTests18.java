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

public class MoveInstanceMethodTests18 extends MoveInstanceMethodTests {

	private static final Class<MoveInstanceMethodTests18> clazz= MoveInstanceMethodTests18.class;

	public MoveInstanceMethodTests18(String name) {
		super(name);
	}

	public static Test suite() {
		return setUpTest(new NoSuperTestsSuite(clazz));
	}

	public static Test setUpTest(Test someTest) {
		return new Java18Setup(someTest);
	}

	// test for bug 410056, move default method from interface to class
	public void test18_1() throws Exception {
		String[] cuQNames= new String[] { "p.A", "p.B" };
		String selectionCuQName= "p.A";
		helper1(cuQNames, selectionCuQName, 3, 20, 3, 34, PARAMETER, "b", true, true);

	}

	// test for bug 410056, move default method from interface to interface
	public void test18_2() throws Exception {
		String[] cuQNames= new String[] { "p.A", "p.B" };
		String selectionCuQName= "p.A";
		helper1(cuQNames, selectionCuQName, 3, 20, 3, 34, PARAMETER, "b", true, true);
	}

	// test for bug 410056, move default method from interface to interface(declared field)
	public void test18_3() throws Exception {
		String[] cuQNames= new String[] { "p.A", "p.B" };
		String selectionCuQName= "p.A";
		helper1(cuQNames, selectionCuQName, 17, 25, 17, 28, FIELD, "fB", true, true);
	}

	// test for bug 410056, move default method from interface to class(declared field)
	public void test18_4() throws Exception {
		String[] cuQNames= new String[] { "p.A", "p.B" };
		String selectionCuQName= "p.A";
		helper1(cuQNames, selectionCuQName, 17, 25, 17, 28, FIELD, "fB", true, true);
	}
}
