/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring.reorg;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.test.performance.Dimension;

import org.eclipse.jdt.ui.tests.refactoring.infra.RefactoringPerformanceTestSetup;

public class MoveCompilationUnitPerfTests1 extends AbstractMoveCompilationUnitPrefTest {

	public static Test suite() {
		// we must make sure that cold is executed before warm
		TestSuite suite= new TestSuite("MoveCompilationUnitPerfTests1");
		suite.addTest(new MoveCompilationUnitPerfTests1("testCold_10_10"));
		suite.addTest(new MoveCompilationUnitPerfTests1("test_10_10"));
		suite.addTest(new MoveCompilationUnitPerfTests1("test_100_10"));
		suite.addTest(new MoveCompilationUnitPerfTests1("test_1000_10"));
		return new RefactoringPerformanceTestSetup(suite);
	}

	public static Test setUpTest(Test someTest) {
		return new RefactoringPerformanceTestSetup(someTest);
	}

	public MoveCompilationUnitPerfTests1(String name) {
		super(name);
	}
	
	public void testCold_10_10() throws Exception {
		executeRefactoring(generateSources(10, 10));
	}
	
	public void test_10_10() throws Exception {
		executeRefactoring(generateSources(10, 10));
	}
	
	public void test_100_10() throws Exception {
		tagAsSummary("Move compilation unit", Dimension.CPU_TIME);
		executeRefactoring(generateSources(100, 10));
	}
	
	public void test_1000_10() throws Exception {
		executeRefactoring(generateSources(1000, 10));
	}
}
