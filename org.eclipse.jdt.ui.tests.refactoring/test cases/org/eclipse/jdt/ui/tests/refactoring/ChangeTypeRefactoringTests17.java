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

import java.util.Collection;

import junit.framework.Test;

import org.eclipse.jdt.testplugin.StringAsserts;

public class ChangeTypeRefactoringTests17 extends ChangeTypeRefactoringTests {

	private static final Class clazz= ChangeTypeRefactoringTests17.class;

	public ChangeTypeRefactoringTests17(String name) {
		super(name);
	}

	public static Test suite() {
		return new Java17Setup(new NoSuperTestsSuite(clazz));
	}

	public static Test setUpTest(Test someTest) {
		return new Java17Setup(someTest);
	}

	@Override
	protected String getTestFileName(boolean positive, boolean input) {
		String fileName= TEST_PATH_PREFIX + getRefactoringPath();

		fileName+= (positive ? "positive17/" : "negative17/");
		fileName += getSimpleTestFileName(input);
		return fileName;
	}

	//--- TESTS
	public void testTryWithResources() throws Exception {
		Collection types= helper1(7, 25, 7, 31, "java.io.InputStreamReader").getValidTypeNames();
		String[] actual= (String[])types.toArray(new String[types.size()]);
		String[] expected= { "java.io.InputStreamReader", "java.io.Reader" };
		StringAsserts.assertEqualStringsIgnoreOrder(actual, expected);
	}

	// tests that are supposed to fail

	public void testUnionType() throws Exception {
		failHelper1(12, 65, 12, 67, 4, "java.lang.Object");
	}
}
