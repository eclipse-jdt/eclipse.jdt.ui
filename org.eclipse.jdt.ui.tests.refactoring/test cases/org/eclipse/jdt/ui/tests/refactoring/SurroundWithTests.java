/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
import junit.framework.TestSuite;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.corext.refactoring.surround.SurroundWithTryCatchRefactoring;

public class SurroundWithTests extends AbstractSelectionTestCase {

	private static SurroundWithTestSetup fgTestSetup;

	public SurroundWithTests(String name) {
		super(name);
	}

	public static Test suite() {
		fgTestSetup= new SurroundWithTestSetup(new TestSuite(SurroundWithTests.class));
		return fgTestSetup;
	}

	public static Test setUpTest(Test someTest) {
		fgTestSetup= new SurroundWithTestSetup(someTest);
		return fgTestSetup;
	}

	protected IPackageFragmentRoot getRoot() {
		return fgTestSetup.getRoot();
	}

	protected String getResourceLocation() {
		return "SurroundWithWorkSpace/SurroundWithTests/";
	}

	protected String adaptName(String name) {
		return Character.toUpperCase(name.charAt(0)) + name.substring(1) + ".java";
	}

	protected void performTest(IPackageFragment packageFragment, String name, String outputFolder, int mode) throws Exception {
		ICompilationUnit unit= createCU(packageFragment, name);
		SurroundWithTryCatchRefactoring refactoring= SurroundWithTryCatchRefactoring.create(unit, getTextSelection());
		String out= null;
		if (mode == COMPARE_WITH_OUTPUT)
			out= getProofedContent(outputFolder, name);
		performTest(unit, refactoring, mode, out, true);
	}

	protected void tryCatchInvalidTest() throws Exception {
		performTest(fgTestSetup.getTryCatchPackage(), getName(), "trycatch_out", INVALID_SELECTION);
	}

	protected void tryCatchTest() throws Exception {
		performTest(fgTestSetup.getTryCatchPackage(), getName(), "trycatch_out", COMPARE_WITH_OUTPUT);
	}

	public void testNoException() throws Exception {
		tryCatchTest();
	}

	public void testAlreadyCaught() throws Exception {
		tryCatchTest();
	}

	public void testInvalidParent1() throws Exception {
		tryCatchInvalidTest();
	}

	public void testInvalidParent2() throws Exception {
		tryCatchInvalidTest();
	}

	public void testThisConstructorCall() throws Exception {
		tryCatchInvalidTest();
	}

	public void testSuperConstructorCall() throws Exception {
		tryCatchInvalidTest();
	}

	public void testSimple() throws Exception {
		tryCatchTest();
	}

	public void testOneLine() throws Exception {
		tryCatchTest();
	}

	public void testMultiLine() throws Exception {
		tryCatchTest();
	}

	public void testExceptionOrder()	throws Exception {
		tryCatchTest();
	}

	public void testWrappedLocal1() throws Exception {
		tryCatchTest();
	}

	public void testWrappedLocal2() throws Exception {
		tryCatchTest();
	}

	public void testWrappedLocal3() throws Exception {
		tryCatchTest();
	}

	public void testWrappedLocal4() throws Exception {
		tryCatchTest();
	}

	public void testWrappedLocal5() throws Exception {
		tryCatchTest();
	}

	public void testWrappedLocal6() throws Exception {
		tryCatchTest();
	}

	public void testWrappedLocal7() throws Exception {
		tryCatchTest();
	}

	public void testWrappedLocal8() throws Exception {
		tryCatchTest();
	}

	public void testInitializerThrowsException() throws Exception {
		tryCatchTest();
	}

	public void testThrowInCatch() throws Exception {
		tryCatchTest();
	}

	public void testExpression() throws Exception {
		tryCatchTest();
	}

	public void testExpressionStatement() throws Exception {
		tryCatchTest();
	}

	public void testInitializer() throws Exception {
		tryCatchTest();
	}

	public void testSuperCall() throws Exception {
		tryCatchTest();
	}

	public void testRuntimeException1() throws Exception {
		tryCatchTest();
	}

	public void testNested() throws Exception {
		tryCatchTest();
	}

	public void testRuntimeException2() throws Exception {
		tryCatchTest();
	}

	public void testStaticField() throws Exception {
		tryCatchInvalidTest();
	}

	public void testDeclarationInitializer() throws Exception {
		 tryCatchTest();
	}

	public void testThenStatement() throws Exception {
		 tryCatchTest();
	}

	public void testEnum1() throws Exception {
		tryCatchTest();
	}

	public void testEnum2() throws Exception {
		tryCatchTest();
	}

	public void testGeneric1() throws Exception {
		tryCatchTest();
	}

	public void testGeneric2() throws Exception {
		tryCatchTest();
	}

	public void testMethodThrowsException() throws Exception {
		tryCatchTest();
	}

	public void testMethodThrowsException1() throws Exception {
		tryCatchTest();
	}
}