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

public class ExtractTempTests13 extends ExtractTempTests {

	private static final Class<ExtractTempTests13> clazz= ExtractTempTests13.class;

	public ExtractTempTests13(String name) {
		super(name);
	}

	public static Test suite() {
		return new Java13Setup(new NoSuperTestsSuite(clazz));
	}

	public static Test setUpTest(Test someTest) {
		return new Java13Setup(someTest);
	}

	@Override
	protected String getTestFileName(boolean canExtract, boolean input) {
		String fileName= TEST_PATH_PREFIX + getRefactoringPath();
		fileName+= canExtract ? "canExtract13/" : "cannotExtract13/";
		return fileName + getSimpleTestFileName(canExtract, input);
	}

	//--- TESTS

	public void test120() throws Exception {
		helper1(6, 19, 9, 22, true, false, "string", "string");
	}
}
