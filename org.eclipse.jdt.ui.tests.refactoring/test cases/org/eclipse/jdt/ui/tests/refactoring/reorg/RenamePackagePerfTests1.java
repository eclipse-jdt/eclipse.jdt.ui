/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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

import org.eclipse.test.OrderedTestSuite;

import org.eclipse.jdt.ui.tests.refactoring.infra.RefactoringPerformanceTestSetup;


public class RenamePackagePerfTests1 extends AbstractRenamePackagePerfTest {

	public static Test suite() {
		// we must make sure that cold is executed before warm
		OrderedTestSuite suite= new OrderedTestSuite(RenamePackagePerfTests1.class, new String[] {
			"testCold_10_10",
			"test_10_10",
			"test_100_10",
			"test_1000_10",
		});
		return new RefactoringPerformanceTestSetup(suite);
	}

	public static Test setUpTest(Test someTest) {
		return new RefactoringPerformanceTestSetup(someTest);
	}

	public RenamePackagePerfTests1(String name) {
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
		// XXX: Removing from fingerprint due to: https://bugs.eclipse.org/bugs/show_bug.cgi?id=266886
//		tagAsSummary("Rename package - 1000 CUs, 10 Refs", Dimension.ELAPSED_PROCESS);
		executeRefactoring(1000, 10, true, 10);
	}
}
