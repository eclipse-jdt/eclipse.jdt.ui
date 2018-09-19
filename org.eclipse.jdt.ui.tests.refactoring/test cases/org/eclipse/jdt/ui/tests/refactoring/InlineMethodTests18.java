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

public class InlineMethodTests18 extends InlineMethodTests {
	private static InlineMethodTestSetup18 fgTestSetup;

	public InlineMethodTests18(String name) {
		super(name);
	}

	public static Test suite() {
		fgTestSetup= new InlineMethodTestSetup18(new NoSuperTestsSuite(InlineMethodTests18.class));
		return fgTestSetup;
	}

	public static Test setUpTest(Test someTest) {
		fgTestSetup= new InlineMethodTestSetup18(someTest);
		return fgTestSetup;
	}

	protected void performSimpleTest() throws Exception {
		performTestInlineCall(fgTestSetup.getSimplePackage(), getName(), COMPARE_WITH_OUTPUT, "simple18_out");
	}

	//--- TESTS

	public void testLambda1() throws Exception {
		performSimpleTest();
	}
}
