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

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class ExtractTempTests18 extends ExtractTempTests {

	private static final Class<ExtractTempTests18> clazz= ExtractTempTests18.class;

	public ExtractTempTests18(String name) {
		super(name);
	}

	public static Test suite() {
		return new Java18Setup(new NoSuperTestsSuite(clazz));
	}

	public static Test setUpTest(Test someTest) {
		return new Java18Setup(someTest);
	}

	@Override
	protected String getTestFileName(boolean canExtract, boolean input) {
		String fileName= TEST_PATH_PREFIX + getRefactoringPath();
		fileName+= canExtract ? "canExtract18/" : "cannotExtract18/";
		return fileName + getSimpleTestFileName(canExtract, input);
	}

	//--- TESTS

	@Override
	public void test110() throws Exception {
		helper1(6, 77, 6, 82, true, false, "x", "x");
	}

	@Override
	public void test111() throws Exception {
		helper1(6, 73, 6, 83, true, false, "foo", "foo");
	}

	public void test112() throws Exception {
		helper1(6, 51, 6, 57, true, false, "x2", "i");
	}

	@Override
	public void test113() throws Exception {
		helper1(7, 28, 7, 34, true, false, "x2", "i");
	}

	@Override
	public void test114() throws Exception {
		helper1(6, 32, 6, 44, true, false, "string", "string");
	}

	@Override
	public void test115() throws Exception {
		helper1(6, 32, 6, 32, true, false, "x2", "x2");
	}

	public void test116() throws Exception {
		helper1(6, 44, 6, 44, true, false, "string", "string");
	}

	public void test117() throws Exception {
		helper1(6, 32, 6, 32, true, false, "integer", "integer");
	}

	public void test118() throws Exception {
		helper1(6, 59, 6, 59, true, false, "string", "string");
	}

	public void test119() throws Exception {
		helper1(7, 30, 7, 63, true, false, "supplier", "supplier");
	}

	// -- testing failing preconditions
	@Override
	public void testFail1() throws Exception {
		failHelper1(6, 32, 6, 58, false, false, "temp", RefactoringStatus.FATAL);
	}

	@Override
	public void testFail2() throws Exception {
		failHelper1(6, 27, 6, 58, false, false, "temp", RefactoringStatus.FATAL);
	}
}
