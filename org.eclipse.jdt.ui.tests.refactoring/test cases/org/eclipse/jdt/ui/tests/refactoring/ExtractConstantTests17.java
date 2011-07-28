/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;


import junit.framework.Test;

public class ExtractConstantTests17 extends ExtractConstantTests {

	private static final Class clazz = ExtractConstantTests17.class;

	public ExtractConstantTests17(String name) {
		super(name);
	}

	public static Test suite() {
		return new Java17Setup(new NoSuperTestsSuite(clazz));
	}

	public static Test setUpTest(Test test) {
		return new Java17Setup(test);
	}

	@Override
	protected String getTestFileName(boolean canExtract, boolean input) {
		String fileName= TEST_PATH_PREFIX + getRefactoringPath();
		fileName+= (canExtract ? "canExtract17/" : "cannotExtract17/");
		return fileName + getSimpleTestFileName(canExtract, input);
	}

	//--- TESTS

	// -- testing failing preconditions
	public void testFail0() throws Exception{
		failHelper1(10, 14, 10, 56, true, true, "CONSTANT");
	}
}

