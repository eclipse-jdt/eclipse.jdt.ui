/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
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

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.corext.refactoring.surround.SurroundWithTryCatchRefactoring;

import junit.framework.Test;

/**
 * Those tests should run on Java Dolphin 1.7 .
 */
public class SurroundWithTests1d7 extends SurroundWithTests {

	private static SurroundWithTestSetup17 fgTestSetup;

	public SurroundWithTests1d7(String name) {
		super(name);
	}

	public static Test suite() {
		fgTestSetup= new SurroundWithTestSetup17(new NoSuperTestsSuite(SurroundWithTests1d7.class));
		return fgTestSetup;
	}

	public static Test setUpTest(Test someTest) {
		fgTestSetup= new SurroundWithTestSetup17(someTest);
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
		performTest(fgTestSetup.getTryCatchPackage(), getName(), "trycatch17_out", INVALID_SELECTION);
	}

	@Override
	protected void tryCatchTest() throws Exception {
		performTest(fgTestSetup.getTryCatchPackage(), getName(), "trycatch17_out", COMPARE_WITH_OUTPUT);
	}

	public void testSimple1() throws Exception {
		tryCatchTest();
	}

	public void testSimple2() throws Exception {
		tryCatchTest();
	}

	public void testMultiTryCatch() throws Exception {
		tryCatchTest();
	}

}