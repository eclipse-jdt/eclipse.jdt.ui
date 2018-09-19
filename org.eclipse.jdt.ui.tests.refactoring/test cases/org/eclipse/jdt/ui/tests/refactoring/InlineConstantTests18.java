/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
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

public class InlineConstantTests18 extends InlineConstantTests {
	private static final Class<InlineConstantTests18> clazz= InlineConstantTests18.class;

	public InlineConstantTests18(String name) {
		super(name);
	}

	@Override
	protected String successPath() {
		return toSucceed ? "/canInline18/" : "/cannotInline18/";
	}

	public static Test suite() {
		return new Java18Setup(new NoSuperTestsSuite(clazz));
	}

	public static Test setUpTest(Test someTest) {
		return new Java18Setup(someTest);
	}

	//--- Test lambda expressions

	@Override
	public void test0() throws Exception {
		helper1("p.TestInlineLambda", 5, 28, 5, 30, true, true);
	}

	@Override
	public void test1() throws Exception {
		helper1("p.TestInlineLambda_Cast", 5, 28, 5, 30, true, true);
	}

	@Override
	public void test2() throws Exception {
		helper1("p.TestInlineLambdaArray", 5, 30, 5, 35, true, true);
	}

	@Override
	public void test3() throws Exception {
		helper1("p.TestInlineLambda_Ambiguous", 5, 28, 5, 30, true, true);
	}

	@Override
	public void test4() throws Exception {
		helper1("p.TestInlineLambda_Ambiguous", 5, 28, 5, 30, true, true);
	}

	@Override
	public void test5() throws Exception {
		helper1("p.TestInlineLambda_Cast", 15, 30, 15, 36, true, true);
	}

	//--- Test method references

	public void test1000() throws Exception {
		helper1("p.TestInlineMethodRef", 5, 28, 5, 30, true, true);
	}

	public void test1001() throws Exception {
		helper1("p.TestInlineMethodRef_Cast", 5, 28, 5, 30, true, true);
	}

	public void test1002() throws Exception {
		helper1("p.TestInlineMethodRefArray", 5, 30, 5, 35, true, true);
	}

	public void test1003() throws Exception {
		helper1("p.TestInlineMethodRef_Ambiguous", 5, 28, 5, 30, true, true);
	}

	public void test1004() throws Exception {
		helper1("p.TestInlineMethodRef_Enum", 5, 28, 5, 30, true, true);
	}
}
