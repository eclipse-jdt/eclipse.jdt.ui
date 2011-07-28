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

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

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
	
	public void test111() throws Exception {
		helper1(8, 16, 8, 33, true, false, "arrayList", "arrayList");
	}
	
	public void test112() throws Exception {
		helper1(8, 20, 8, 37, true, false, "arrayList", "arrayList");
	}
	
	public void test113() throws Exception {
		helper1(12, 16, 12, 33, true, false, "arrayList2", "arrayList2");
	}

	public void test114() throws Exception {
		helper1(9, 34, 9, 56, true, false, "fileReader", "fileReader");
	}

	// -- testing failing preconditions
	public void testFail1() throws Exception {
		failHelper1(9, 14, 9, 56, false, false, "temp", RefactoringStatus.FATAL);
	}
}
