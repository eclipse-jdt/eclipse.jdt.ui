/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
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

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.corext.refactoring.surround.SurroundWithTryCatchRefactoring;

import junit.framework.Test;

public class SurroundWithTests18 extends SurroundWithTests {

	private static SurroundWithTestSetup18 fgTestSetup;

	public SurroundWithTests18(String name) {
		super(name);
	}

	public static Test suite() {
		fgTestSetup= new SurroundWithTestSetup18(new NoSuperTestsSuite(SurroundWithTests18.class));
		return fgTestSetup;
	}

	public static Test setUpTest(Test someTest) {
		fgTestSetup= new SurroundWithTestSetup18(someTest);
		return fgTestSetup;
	}

	@Override
	protected IPackageFragmentRoot getRoot() {
		return fgTestSetup.getRoot();
	}

	@Override
	protected SurroundWithTryCatchRefactoring createRefactoring(ICompilationUnit unit) {
		return SurroundWithTryCatchRefactoring.create(unit, getTextSelection(), true);
	}

	@Override
	protected void tryCatchInvalidTest() throws Exception {
		performTest(fgTestSetup.getTryCatchPackage(), getName(), "trycatch18_out", INVALID_SELECTION);
	}

	@Override
	protected void tryCatchTest() throws Exception {
		performTest(fgTestSetup.getTryCatchPackage(), getName(), "trycatch18_out", COMPARE_WITH_OUTPUT);
	}

	public void testSimple1() throws Exception {
		tryCatchTest();
	}

	public void testLambda1() throws Exception {
		tryCatchTest();
	}

	public void testLambda2() throws Exception {
		tryCatchInvalidTest();
	}

	public void testLambda3() throws Exception {
		tryCatchTest();
	}

	public void testLambda4() throws Exception {
		tryCatchTest();
	}

	public void testMethodReference1() throws Exception {
		tryCatchTest();
	}

	public void testMethodReference2() throws Exception {
		tryCatchTest();
	}

	public void testMethodReference3() throws Exception {
		tryCatchInvalidTest();
	}

	public void testMethodReference4() throws Exception {
		tryCatchTest();
	}

	public void testMethodReference5() throws Exception {
		tryCatchInvalidTest();
	}

	public void testMethodReference6() throws Exception {
		tryCatchTest();
	}
}
