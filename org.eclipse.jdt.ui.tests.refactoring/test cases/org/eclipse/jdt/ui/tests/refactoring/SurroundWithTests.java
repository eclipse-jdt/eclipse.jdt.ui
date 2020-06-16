/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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

import static org.eclipse.jdt.ui.tests.refactoring.AbstractJunit4SelectionTestCase.TestMode.COMPARE_WITH_OUTPUT;
import static org.eclipse.jdt.ui.tests.refactoring.AbstractJunit4SelectionTestCase.TestMode.INVALID_SELECTION;

import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.corext.refactoring.surround.SurroundWithTryCatchRefactoring;

public class SurroundWithTests extends AbstractJunit4SelectionTestCase {

	@Rule
	public SurroundWithTestSetup fgTestSetup=new SurroundWithTestSetup();

	protected IPackageFragmentRoot getRoot() {
		return fgTestSetup.getRoot();
	}

	@Override
	protected String getResourceLocation() {
		return "SurroundWithWorkSpace/SurroundWithTests/";
	}

	@Override
	protected String adaptName(String name) {
		return Character.toUpperCase(name.charAt(0)) + name.substring(1) + ".java";
	}

	protected void performTest(IPackageFragment packageFragment, String name, String outputFolder, TestMode mode) throws Exception {
		ICompilationUnit unit= createCU(packageFragment, name);
		SurroundWithTryCatchRefactoring refactoring= createRefactoring(unit);
		String out= null;
		if (mode == COMPARE_WITH_OUTPUT)
			out= getProofedContent(outputFolder, name);
		performTest(unit, refactoring, mode, out, true);
	}

	protected SurroundWithTryCatchRefactoring createRefactoring(ICompilationUnit unit) {
		return SurroundWithTryCatchRefactoring.create(unit, getTextSelection());
	}

	protected void tryCatchInvalidTest() throws Exception {
		performTest(fgTestSetup.getTryCatchPackage(), getName(), "trycatch_out", INVALID_SELECTION);
	}

	protected void tryCatchTest() throws Exception {
		performTest(fgTestSetup.getTryCatchPackage(), getName(), "trycatch_out", COMPARE_WITH_OUTPUT);
	}

	@Test
	public void testNoException() throws Exception {
		tryCatchTest();
	}

	@Test
	public void testAlreadyCaught() throws Exception {
		tryCatchTest();
	}

	@Test
	public void testInvalidParent1() throws Exception {
		tryCatchInvalidTest();
	}

	@Test
	public void testInvalidParent2() throws Exception {
		tryCatchInvalidTest();
	}

	@Test
	public void testThisConstructorCall() throws Exception {
		tryCatchInvalidTest();
	}

	@Test
	public void testSuperConstructorCall() throws Exception {
		tryCatchInvalidTest();
	}

	@Test
	public void testSimple() throws Exception {
		tryCatchTest();
	}

	@Test
	public void testOneLine() throws Exception {
		tryCatchTest();
	}

	@Test
	public void testMultiLine() throws Exception {
		tryCatchTest();
	}

	@Test
	public void testExceptionOrder()	throws Exception {
		tryCatchTest();
	}

	@Test
	public void testWrappedLocal1() throws Exception {
		tryCatchTest();
	}

	@Test
	public void testWrappedLocal2() throws Exception {
		tryCatchTest();
	}

	@Test
	public void testWrappedLocal3() throws Exception {
		tryCatchTest();
	}

	@Test
	public void testWrappedLocal4() throws Exception {
		tryCatchTest();
	}

	@Test
	public void testWrappedLocal5() throws Exception {
		tryCatchTest();
	}

	@Test
	public void testWrappedLocal6() throws Exception {
		tryCatchTest();
	}

	@Test
	public void testWrappedLocal7() throws Exception {
		tryCatchTest();
	}

	@Test
	public void testWrappedLocal8() throws Exception {
		tryCatchTest();
	}

	@Test
	public void testInitializerThrowsException() throws Exception {
		tryCatchTest();
	}

	@Test
	public void testThrowInCatch() throws Exception {
		tryCatchTest();
	}

	@Test
	public void testExpression() throws Exception {
		tryCatchTest();
	}

	@Test
	public void testExpressionStatement() throws Exception {
		tryCatchTest();
	}

	@Test
	public void testInitializer() throws Exception {
		tryCatchTest();
	}

	@Test
	public void testSuperCall() throws Exception {
		tryCatchTest();
	}

	@Test
	public void testRuntimeException1() throws Exception {
		tryCatchTest();
	}

	@Test
	public void testNested() throws Exception {
		tryCatchTest();
	}

	@Test
	public void testRuntimeException2() throws Exception {
		tryCatchTest();
	}

	@Test
	public void testStaticField() throws Exception {
		tryCatchInvalidTest();
	}

	@Test
	public void testDeclarationInitializer() throws Exception {
		 tryCatchTest();
	}

	@Test
	public void testThenStatement() throws Exception {
		 tryCatchTest();
	}

	@Test
	public void testEnum1() throws Exception {
		tryCatchTest();
	}

	@Test
	public void testEnum2() throws Exception {
		tryCatchTest();
	}

	@Test
	public void testGeneric1() throws Exception {
		tryCatchTest();
	}

	@Test
	public void testGeneric2() throws Exception {
		tryCatchTest();
	}

	@Test
	public void testMethodThrowsException() throws Exception {
		tryCatchTest();
	}

	@Test
	public void testMethodThrowsException1() throws Exception {
		tryCatchTest();
	}
}