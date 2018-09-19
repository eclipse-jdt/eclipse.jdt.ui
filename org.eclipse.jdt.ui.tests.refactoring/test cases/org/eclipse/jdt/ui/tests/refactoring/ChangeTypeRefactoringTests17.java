/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
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

import java.util.Collection;

import org.eclipse.jdt.testplugin.StringAsserts;

import junit.framework.Test;

public class ChangeTypeRefactoringTests17 extends ChangeTypeRefactoringTests {

	private static final Class<ChangeTypeRefactoringTests17> clazz= ChangeTypeRefactoringTests17.class;

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
		Collection<String> types= helper1(7, 25, 7, 31, "java.io.InputStreamReader").getValidTypeNames();
		String[] actual= types.toArray(new String[types.size()]);
		String[] expected= { "java.io.InputStreamReader", "java.io.Reader" };
		StringAsserts.assertEqualStringsIgnoreOrder(actual, expected);
	}

	// tests that are supposed to fail

	public void testUnionType() throws Exception {
		failHelper1(12, 65, 12, 67, 4, "java.lang.Object");
	}
}
