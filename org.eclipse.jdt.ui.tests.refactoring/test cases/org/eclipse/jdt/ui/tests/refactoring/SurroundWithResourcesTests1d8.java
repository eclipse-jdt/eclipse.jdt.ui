/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial implementation based on SurroundWithTests1d8
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import static org.eclipse.jdt.ui.tests.refactoring.AbstractJunit4SelectionTestCase.TestMode.COMPARE_WITH_OUTPUT;
import static org.eclipse.jdt.ui.tests.refactoring.AbstractJunit4SelectionTestCase.TestMode.INVALID_SELECTION;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.corext.refactoring.surround.SurroundWithTryWithResourcesRefactoring;

import org.eclipse.jdt.ui.tests.CustomBaseRunner;
import org.eclipse.jdt.ui.tests.IgnoreInheritedTests;

/**
 * Those tests are made to run on Java Spider 1.8 .
 */
@IgnoreInheritedTests
@RunWith(CustomBaseRunner.class)
public class SurroundWithResourcesTests1d8 extends AbstractJunit4SelectionTestCase {

	@Rule
	public SurroundWithResourcesTestSetup1d8 fgTestSetup=new SurroundWithResourcesTestSetup1d8();

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
		SurroundWithTryWithResourcesRefactoring refactoring= createRefactoring(unit);
		String out= null;
		if (mode == COMPARE_WITH_OUTPUT)
			out= getProofedContent(outputFolder, name);
		performTest(unit, refactoring, mode, out, true);
	}

	protected SurroundWithTryWithResourcesRefactoring createRefactoring(ICompilationUnit unit) {
		return SurroundWithTryWithResourcesRefactoring.create(unit, getTextSelection());
	}

	protected void tryResourcesInvalidTest() throws Exception {
		performTest(fgTestSetup.getTryCatchPackage(), getName(), "tryresources18_out", INVALID_SELECTION);
	}

	protected void tryResourcesTest() throws Exception {
		performTest(fgTestSetup.getTryCatchPackage(), getName(), "tryresources18_out", COMPARE_WITH_OUTPUT);
	}

	@Test
	public void testSimple1() throws Exception {
		tryResourcesTest();
	}

	@Test
	public void testSimple2() throws Exception {
		tryResourcesTest();
	}

	@Test
	public void testSimple3() throws Exception {
		tryResourcesTest();
	}

	@Test
	public void testSimple4() throws Exception {
		tryResourcesTest();
	}

	@Test
	public void testNonClosableInserted1() throws Exception {
		tryResourcesTest();
	}

	@Test
	public void testWithException1() throws Exception {
		tryResourcesTest();
	}

	@Test
	public void testWithException2() throws Exception {
		tryResourcesTest();
	}

	@Test
	public void testWithThrows1() throws Exception {
		tryResourcesTest();
	}

	@Test
	public void testWithThrows2() throws Exception {
		tryResourcesTest();
	}

	@Test
	public void testWithThrows3() throws Exception {
		tryResourcesTest();
	}

	@Test
	public void testInvalidStatement1() throws Exception {
		tryResourcesInvalidTest();
	}

	@Test
	public void testInvalidParent1() throws Exception {
		tryResourcesInvalidTest();
	}

	@Test
	public void testInvalidParent2() throws Exception {
		tryResourcesInvalidTest();
	}

	@Test
	public void testMethodThrowsException1() throws Exception {
		tryResourcesTest();
	}

	@Test
	public void testMethodThrowsException2() throws Exception {
		tryResourcesTest();
	}

	@Test
	public void testInvalidTryResources1() throws Exception {
		tryResourcesInvalidTest();
	}

	@Test
	public void testExceptionFilter1() throws Exception {
		tryResourcesTest();
	}

	@Test
	public void testLambda1() throws Exception {
		tryResourcesTest();
	}

	@Test
	public void testLambda2() throws Exception {
		tryResourcesInvalidTest();
	}

	@Test
	public void testLambda3() throws Exception {
		tryResourcesTest();
	}

	@Test
	public void testLambda4() throws Exception {
		tryResourcesInvalidTest();
	}

	@Test
	public void testMethodReference1() throws Exception {
		tryResourcesInvalidTest();
	}

	@Test
	public void testTry1() throws Exception {
		tryResourcesTest();
	}

	@Test
	public void testTry2() throws Exception {
		tryResourcesTest();
	}

}
