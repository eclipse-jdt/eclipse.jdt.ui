/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * This is an implementation of an early-draft specification developed under the Java
 * Community Process (JCP) and is made available for testing and evaluation purposes
 * only. The code is not compatible with any specification of the JCP.
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import junit.framework.Test;

public class ExtractMethodTests17 extends ExtractMethodTests {
	private static ExtractMethodTestSetup17 fgTestSetup;

	public ExtractMethodTests17(String name) {
		super(name);
	}

	public static Test suite() {
		fgTestSetup= new ExtractMethodTestSetup17(new NoSuperTestsSuite(ExtractMethodTests17.class));
		return fgTestSetup;
	}

	public static Test setUpTest(Test test) {
		fgTestSetup= new ExtractMethodTestSetup17(test);
		return fgTestSetup;
	}

	protected void try17Test() throws Exception {
		performTest(fgTestSetup.getTry17Package(), "A", COMPARE_WITH_OUTPUT, "try17_out");
	}

	//====================================================================================
	// Testing Extracted result
	//====================================================================================

	//---- Test Try / catch block

	public void test1() throws Exception {
		try17Test();
	}

	public void test2() throws Exception {
		try17Test();
	}

	public void test3() throws Exception {
		try17Test();
	}

	public void test4() throws Exception {
		try17Test();
	}

	public void test5() throws Exception {
		try17Test();
	}

	public void test6() throws Exception {
		try17Test();
	}
}
