/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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


public class RenameMethodPerfTests1 extends AbstractRenameMethodPerfTest {

	public static Test suite() {
		// we must make sure that cold is executed before warm
		TestSuite suite= new TestSuite("RenameTypePerfAcceptanceTests1");
		suite.addTest(new RenameMethodPerfTests1("testCold_10_10"));
		suite.addTest(new RenameMethodPerfTests1("test_10_10"));
		suite.addTest(new RenameMethodPerfTests1("test_100_10"));
		suite.addTest(new RenameMethodPerfTests1("test_1000_10"));
		return new RefactoringPerformanceTestSetup(suite);
	}

	public static Test setUpTest(Test someTest) {
		return new RefactoringPerformanceTestSetup(someTest);
	}

	public RenameMethodPerfTests1(String name) {
		super(name);
	}

	public void testCold_10_10() throws Exception {
		executeRefactoring(10, 10, false, 3);
	}

	public void test_10_10() throws Exception {
		executeRefactoring(10, 10, true, 10);
	}

	public void test_100_10() throws Exception {
		executeRefactoring(100, 10, true, 10);
	}

	public void test_1000_10() throws Exception {
		tagAsSummary("Rename method - 1000 CUs, 10 Refs", Dimension.ELAPSED_PROCESS);
		executeRefactoring(1000, 10, true, 10);
	}
}
