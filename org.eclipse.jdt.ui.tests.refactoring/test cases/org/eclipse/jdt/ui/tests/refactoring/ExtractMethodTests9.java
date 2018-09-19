/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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

public class ExtractMethodTests9 extends ExtractMethodTests {
	private static ExtractMethodTestSetup9 fgTestSetup;

	public ExtractMethodTests9(String name) {
		super(name);
	}

	public static Test suite() {
		fgTestSetup= new ExtractMethodTestSetup9(new NoSuperTestsSuite(ExtractMethodTests9.class));
		return fgTestSetup;
	}

	public static Test setUpTest(Test test) {
		fgTestSetup= new ExtractMethodTestSetup9(test);
		return fgTestSetup;
	}

	protected void try9Test() throws Exception {
		performTest(fgTestSetup.getTry9Package(), "A", COMPARE_WITH_OUTPUT, "try9_out");
	}

	@Override
	protected void invalidSelectionTest() throws Exception {
		performTest(fgTestSetup.getInvalidSelectionPackage(), "A", INVALID_SELECTION, null);
	}

	//====================================================================================
	// Testing invalid selections
	//====================================================================================

	@Override
	public void test101() throws Exception {
		invalidSelectionTest();
	}

	//====================================================================================
	// Testing try-with-resources
	//====================================================================================

	@Override
	public void test201() throws Exception {
		try9Test();
	}

}
