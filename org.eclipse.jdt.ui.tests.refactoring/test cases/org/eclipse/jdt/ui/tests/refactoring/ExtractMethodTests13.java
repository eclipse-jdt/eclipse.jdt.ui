/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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

public class ExtractMethodTests13 extends ExtractMethodTests {
	private static ExtractMethodTestSetup13 fgTestSetup;

	public ExtractMethodTests13(String name) {
		super(name);
	}

	public static Test suite() {
		fgTestSetup= new ExtractMethodTestSetup13(new NoSuperTestsSuite(ExtractMethodTests13.class));
		return fgTestSetup;
	}

	public static Test setUpTest(Test test) {
		fgTestSetup= new ExtractMethodTestSetup13(test);
		return fgTestSetup;
	}

	@Override
	protected void invalidSelectionTest() throws Exception {
		performTest(fgTestSetup.getInvalidSelectionPackage(), "A", INVALID_SELECTION, null);
	}

	protected void try13Test() throws Exception {
		performTest(fgTestSetup.getTry13Package(), "A", COMPARE_WITH_OUTPUT, "try13_out");
	}

	public void testSwitchExpr1() throws Exception {
		try13Test();
	}

	public void testSwitchExpr2() throws Exception {
		invalidSelectionTest();
	}

}
