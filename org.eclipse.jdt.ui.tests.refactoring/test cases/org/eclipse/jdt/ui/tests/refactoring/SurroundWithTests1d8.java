/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
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
import org.junit.runner.RunWith;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.corext.refactoring.surround.SurroundWithTryCatchRefactoring;

import org.eclipse.jdt.ui.tests.CustomBaseRunner;
import org.eclipse.jdt.ui.tests.IgnoreInheritedTests;

/**
 * Those tests are made to run on Java Spider 1.8 .
 */
@IgnoreInheritedTests
@RunWith(CustomBaseRunner.class)
public class SurroundWithTests1d8 extends SurroundWithTests {

	@Rule
	public SurroundWithTestSetup1d8 fGTestSetup1d8= new SurroundWithTestSetup1d8();

	@Override
	protected IPackageFragmentRoot getRoot() {
		return fGTestSetup1d8.getRoot();
	}

	@Override
	protected SurroundWithTryCatchRefactoring createRefactoring(ICompilationUnit unit) {
		return SurroundWithTryCatchRefactoring.create(unit, getTextSelection(), true);
	}

	@Override
	protected void tryCatchInvalidTest() throws Exception {
		performTest(fGTestSetup1d8.getTryCatchPackage(), getName(), "trycatch18_out", INVALID_SELECTION);
	}

	@Override
	protected void tryCatchTest() throws Exception {
		performTest(fGTestSetup1d8.getTryCatchPackage(), getName(), "trycatch18_out", COMPARE_WITH_OUTPUT);
	}

	@Test
	public void testSimple1() throws Exception {
		tryCatchTest();
	}

	@Test
	public void testLambda1() throws Exception {
		tryCatchTest();
	}

	@Test
	public void testLambda2() throws Exception {
		tryCatchInvalidTest();
	}

	@Test
	public void testLambda3() throws Exception {
		tryCatchTest();
	}

	@Test
	public void testLambda4() throws Exception {
		tryCatchTest();
	}

	@Test
	public void testMethodReference1() throws Exception {
		tryCatchTest();
	}

	@Test
	public void testMethodReference2() throws Exception {
		tryCatchTest();
	}

	@Test
	public void testMethodReference3() throws Exception {
		tryCatchInvalidTest();
	}

	@Test
	public void testMethodReference4() throws Exception {
		tryCatchTest();
	}

	@Test
	public void testMethodReference5() throws Exception {
		tryCatchInvalidTest();
	}

	@Test
	public void testMethodReference6() throws Exception {
		tryCatchTest();
	}
}
