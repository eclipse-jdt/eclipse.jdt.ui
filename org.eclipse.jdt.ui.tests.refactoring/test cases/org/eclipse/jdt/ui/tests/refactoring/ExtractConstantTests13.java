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

public class ExtractConstantTests13 extends ExtractConstantTests {

	private static final Class<ExtractConstantTests13> clazz = ExtractConstantTests13.class;

	public ExtractConstantTests13(String name) {
		super(name);
	}

	public static Test suite() {
		return new Java13Setup(new NoSuperTestsSuite(clazz));
	}

	public static Test setUpTest(Test test) {
		return new Java13Setup(test);
	}

	@Override
	protected String getTestFileName(boolean canExtract, boolean input) {
		String fileName= TEST_PATH_PREFIX + getRefactoringPath();
		fileName+= (canExtract ? "canExtract13/" : "cannotExtract13/");
		return fileName + getSimpleTestFileName(canExtract, input);
	}

	//--- TESTS

	@Override
	public void test0() throws Exception {
		helper1(6, 19, 9, 22, true, false, "CONSTANT", "STRING");
	}
}

