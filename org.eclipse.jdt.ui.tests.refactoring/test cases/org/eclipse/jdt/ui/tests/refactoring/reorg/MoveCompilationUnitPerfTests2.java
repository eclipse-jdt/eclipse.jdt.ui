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
package org.eclipse.jdt.ui.tests.refactoring.reorg;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.test.performance.Dimension;

import org.eclipse.jdt.ui.tests.refactoring.infra.RefactoringPerformanceTestSetup;

public class MoveCompilationUnitPerfTests2 extends AbstractMoveCompilationUnitPrefTest {

	public static Test suite() {
		// we must make sure that cold is executed before warm
		TestSuite suite= new TestSuite("MoveCompilationUnitPerfTests2");
		suite.addTest(new MoveCompilationUnitPerfTests2("testCold_10_10"));
		suite.addTest(new MoveCompilationUnitPerfTests2("test_10_10"));
		suite.addTest(new MoveCompilationUnitPerfTests2("test_10_100"));
		suite.addTest(new MoveCompilationUnitPerfTests2("test_10_1000"));
		return new RefactoringPerformanceTestSetup(suite);
	}

	public static Test setUpTest(Test someTest) {
		return new RefactoringPerformanceTestSetup(someTest);
	}

	public MoveCompilationUnitPerfTests2(String name) {
		super(name);
	}

	public void testCold_10_10() throws Exception {
		executeRefactoring(10, 10, false, 3);
	}

	public void test_10_10() throws Exception {
		executeRefactoring(10, 10, true, 3);
	}

	public void test_10_100() throws Exception {
		executeRefactoring(10, 100, true, 1);
	}

	public void test_10_1000() throws Exception {
		tagAsSummary("Move compilation units - 10 CUs, 1000 Refs", Dimension.ELAPSED_PROCESS);
		executeRefactoring(10, 1000, true, 1);
	}
}
