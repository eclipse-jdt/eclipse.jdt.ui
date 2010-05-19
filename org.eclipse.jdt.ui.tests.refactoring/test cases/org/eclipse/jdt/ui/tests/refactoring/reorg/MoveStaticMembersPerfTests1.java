/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
import org.eclipse.test.performance.Performance;

import org.eclipse.jdt.ui.tests.refactoring.infra.RefactoringPerformanceTestSetup;

public class MoveStaticMembersPerfTests1 extends AbstractMoveStaticMemberPrefTest {

	public static Test suite() {
		// we must make sure that cold is executed before warm
		TestSuite suite= new TestSuite("MoveStaticMembersPerfTests1");
		suite.addTest(new MoveStaticMembersPerfTests1("testCold_10_10"));
		suite.addTest(new MoveStaticMembersPerfTests1("test_10_10"));
		suite.addTest(new MoveStaticMembersPerfTests1("test_100_10"));
		suite.addTest(new MoveStaticMembersPerfTests1("test_1000_10"));
		return new RefactoringPerformanceTestSetup(suite);
	}

	public static Test setUpTest(Test someTest) {
		return new RefactoringPerformanceTestSetup(someTest);
	}

	public MoveStaticMembersPerfTests1(String name) {
		super(name);
	}

	public void testCold_10_10() throws Exception {
		executeRefactoring(10, 10, false, 3);
	}

	public void test_10_10() throws Exception {
		executeRefactoring(10, 10, true, 3);
	}

	public void test_100_10() throws Exception {
		setComment(Performance.EXPLAINS_DEGRADATION_COMMENT, "Performance slowed down because the code in the baseline was not correct and got fixed in 3.6 (see bug 304193)");
		executeRefactoring(100, 10, true, 1);
	}

	public void test_1000_10() throws Exception {
		tagAsSummary("Move static members - 1000 CUs, 10 Refs", Dimension.ELAPSED_PROCESS);
		setComment(Performance.EXPLAINS_DEGRADATION_COMMENT, "Performance slowed down because the code in the baseline was not correct and got fixed in 3.6");
		executeRefactoring(1000, 10, true, 1);
	}
}
