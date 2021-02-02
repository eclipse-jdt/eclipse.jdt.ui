/*******************************************************************************
 * Copyright (c) 2011, 2021 IBM Corporation and others.
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
 *     Samrat Dhillon <samrat.dhillon@gmail.com> - Bug 388724 -  [surround with try/catch][quick fix] Multi-Catch QuickFix creates compiler error
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
 * Those tests should run on Java Dolphin 1.7 .
 */
@IgnoreInheritedTests
@RunWith(CustomBaseRunner.class)
public class SurroundWithTests1d7 extends SurroundWithTests {
	@Rule
	public SurroundWithTestSetup17 fgTestSetup1d7= new SurroundWithTestSetup17();

	@Override
	protected IPackageFragmentRoot getRoot() {
		return fgTestSetup1d7.getRoot();
	}

	@Override
	protected SurroundWithTryCatchRefactoring createRefactoring(ICompilationUnit unit) {
		return SurroundWithTryCatchRefactoring.create(unit, getTextSelection(), true);
	}

	@Override
	protected void tryCatchInvalidTest() throws Exception {
		performTest(fgTestSetup1d7.getTryCatchPackage(), getName(), "trycatch17_out", INVALID_SELECTION);
	}

	@Override
	protected void tryCatchTest() throws Exception {
		performTest(fgTestSetup1d7.getTryCatchPackage(), getName(), "trycatch17_out", COMPARE_WITH_OUTPUT);
	}

	@Test
	public void testSimple1() throws Exception {
		tryCatchTest();
	}

	@Test
	public void testSimple2() throws Exception {
		tryCatchTest();
	}

	@Test
	public void testMultiTryCatch() throws Exception {
		tryCatchTest();
	}
}
