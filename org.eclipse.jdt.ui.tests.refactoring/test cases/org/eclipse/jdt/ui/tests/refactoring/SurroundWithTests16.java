/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
 *     Red Hat Inc. - copied and modified from SurroundWithTests1d7
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
 * Those tests should run on Java 15.
 */
@IgnoreInheritedTests
@RunWith(CustomBaseRunner.class)
public class SurroundWithTests16 extends SurroundWithTests {
	@Rule
	public SurroundWithTestSetup16 fgTestSetup16= new SurroundWithTestSetup16();

	@Override
	protected IPackageFragmentRoot getRoot() {
		return fgTestSetup16.getRoot();
	}

	@Override
	protected SurroundWithTryCatchRefactoring createRefactoring(ICompilationUnit unit) {
		return SurroundWithTryCatchRefactoring.create(unit, getTextSelection(), true);
	}

	@Override
	protected void tryCatchInvalidTest() throws Exception {
		performTest(fgTestSetup16.getTryCatchPackage(), getName(), "trycatch16_out", INVALID_SELECTION);
	}

	@Override
	protected void tryCatchTest() throws Exception {
		performTest(fgTestSetup16.getTryCatchPackage(), getName(), "trycatch16_out", COMPARE_WITH_OUTPUT);
	}

	@Test
	public void testBug566949_1() throws Exception {
		tryCatchTest();
	}

	@Test
	public void testBug566949_2() throws Exception {
		tryCatchTest();
	}

}
