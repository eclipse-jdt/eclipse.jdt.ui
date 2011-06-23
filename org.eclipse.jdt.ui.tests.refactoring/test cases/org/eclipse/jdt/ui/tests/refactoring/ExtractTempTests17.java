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

public class ExtractTempTests17 extends ExtractTempTests {

	private static final Class clazz= ExtractTempTests17.class;

	public ExtractTempTests17(String name) {
		super(name);
	}

	public static Test suite() {
		return new Java17Setup(new NoSuperTestsSuite(clazz));
	}

	public static Test setUpTest(Test someTest) {
		return new Java17Setup(someTest);
	}

	@Override
	protected String getTestFileName(boolean canExtract, boolean input){
		String fileName= TEST_PATH_PREFIX + getRefactoringPath();
		fileName+= canExtract ? "canExtract17/" : "cannotExtract17/";
		return fileName + getSimpleTestFileName(canExtract, input);
	}

	//--- TESTS

	public void test110() throws Exception {
		helper1(14, 13, 14, 15, true, false, "temp", "ex2");
	}
}
