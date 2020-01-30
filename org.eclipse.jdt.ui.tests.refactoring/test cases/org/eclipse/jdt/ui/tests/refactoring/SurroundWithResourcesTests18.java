/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial implementation based on SurroundWithTests18
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.corext.refactoring.surround.SurroundWithTryWithResourcesRefactoring;

import junit.framework.Test;

public class SurroundWithResourcesTests18 extends AbstractSelectionTestCase {

	private static SurroundWithResourcesTestSetup18 fgTestSetup;

	public SurroundWithResourcesTests18(String name) {
		super(name);
	}

	public static Test suite() {
		fgTestSetup= new SurroundWithResourcesTestSetup18(new NoSuperTestsSuite(SurroundWithResourcesTests18.class));
		return fgTestSetup;
	}

	public static Test setUpTest(Test someTest) {
		fgTestSetup= new SurroundWithResourcesTestSetup18(someTest);
		return fgTestSetup;
	}

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

	protected void performTest(IPackageFragment packageFragment, String name, String outputFolder, int mode) throws Exception {
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

	public void testSimple1() throws Exception {
		tryResourcesTest();
	}

	public void testSimple2() throws Exception {
		tryResourcesTest();
	}

	public void testSimple3() throws Exception {
		tryResourcesTest();
	}

	public void testSimple4() throws Exception {
		tryResourcesTest();
	}

	public void testNonClosableInserted1() throws Exception {
		tryResourcesTest();
	}

	public void testWithException1() throws Exception {
		tryResourcesTest();
	}

	public void testWithException2() throws Exception {
		tryResourcesTest();
	}

	public void testWithThrows1() throws Exception {
		tryResourcesTest();
	}

	public void testInvalidStatement1() throws Exception {
		tryResourcesInvalidTest();
	}

	public void testInvalidParent1() throws Exception {
		tryResourcesInvalidTest();
	}

	public void testInvalidParent2() throws Exception {
		tryResourcesInvalidTest();
	}

	public void testMethodThrowsException1() throws Exception {
		tryResourcesTest();
	}

	public void testMethodThrowsException2() throws Exception {
		tryResourcesTest();
	}

	public void testInvalidTryResources1() throws Exception {
		tryResourcesInvalidTest();
	}

	public void testExceptionFilter1() throws Exception {
		tryResourcesTest();
	}

	public void testLambda1() throws Exception {
		tryResourcesTest();
	}

	public void testLambda2() throws Exception {
		tryResourcesInvalidTest();
	}

	public void testLambda3() throws Exception {
		tryResourcesTest();
	}

	public void testLambda4() throws Exception {
		tryResourcesInvalidTest();
	}

	public void testMethodReference1() throws Exception {
		tryResourcesInvalidTest();
	}

}
