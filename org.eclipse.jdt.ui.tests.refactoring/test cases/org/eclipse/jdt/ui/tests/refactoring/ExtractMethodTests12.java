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

public class ExtractMethodTests12 extends ExtractMethodTests {
	private static ExtractMethodTestSetup12 fgTestSetup;

	public ExtractMethodTests12(String name) {
		super(name);
	}

	public static Test suite() {
		fgTestSetup= new ExtractMethodTestSetup12(new NoSuperTestsSuite(ExtractMethodTests12.class));
		return fgTestSetup;
	}

	public static Test setUpTest(Test test) {
		fgTestSetup= new ExtractMethodTestSetup12(test);
		return fgTestSetup;
	}

	protected void try12Test() throws Exception {
		performTest(fgTestSetup.getTry12Package(), "A", COMPARE_WITH_OUTPUT, "try12_out");
	}

	//====================================================================================
	// Testing var type 
	//====================================================================================

	public void testSwitchExpr1() throws Exception {
		try12Test();
	}

}
