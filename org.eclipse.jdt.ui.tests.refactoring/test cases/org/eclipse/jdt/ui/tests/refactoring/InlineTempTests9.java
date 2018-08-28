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

public class InlineTempTests9 extends InlineTempTests {

	private static final Class<InlineTempTests9> clazz= InlineTempTests9.class;

	public InlineTempTests9(String name) {
		super(name);
	}

	public static Test suite() {
		return new Java9Setup(new NoSuperTestsSuite(clazz));
	}

	public static Test setUpTest(Test someTest) {
		return new Java9Setup(someTest);
	}

	@Override
	protected String getTestFileName(boolean canInline, boolean input) {
		String fileName= TEST_PATH_PREFIX + getRefactoringPath();
		fileName+= (canInline ? "canInline9/" : "cannotInline9/");
		return fileName + getSimpleTestFileName(canInline, input);
	}

	//--- tests for try-with-resources

	@Override
	public void test0() throws Exception {
		helper2(9, 19, 9, 28);
	}

	@Override
	public void test1() throws Exception {
		helper2(10, 37, 10, 46);
	}

}