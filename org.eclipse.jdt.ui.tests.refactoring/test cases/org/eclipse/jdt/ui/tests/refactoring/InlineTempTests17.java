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

public class InlineTempTests17 extends InlineTempTests {

	private static final Class clazz= InlineTempTests17.class;

	public InlineTempTests17(String name) {
		super(name);
	}

	public static Test suite() {
		return new Java17Setup(new NoSuperTestsSuite(clazz));
	}

	public static Test setUpTest(Test someTest) {
		return new Java17Setup(someTest);
	}

	@Override
	protected String getTestFileName(boolean canInline, boolean input){
		String fileName= TEST_PATH_PREFIX + getRefactoringPath();
		fileName += (canInline ? "canInline17/": "cannotInline17/");
		return fileName + getSimpleTestFileName(canInline, input);
	}

	//--- tests

	public void test0() throws Exception{
		helper1(8, 19, 8, 23);
	}
}
