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

import org.eclipse.jdt.ui.tests.refactoring.infra.AbstractRefactoringTestSetup;

public class RenamePackagePerfTests2 extends AbstractRenamePackagePerfTest {

	public static Test suite() {
		// we must make sure that cold is executed before warm
		TestSuite suite= new TestSuite("RenamePackagePerfTests2");
		suite.addTest(new RenamePackagePerfTests2("testCold_10_10"));
		suite.addTest(new RenamePackagePerfTests2("test_10_10"));
		suite.addTest(new RenamePackagePerfTests2("test_10_100"));
		suite.addTest(new RenamePackagePerfTests2("test_10_1000"));
		return new AbstractRefactoringTestSetup(suite);
	}

	public static Test setUpTest(Test someTest) {
		return new AbstractRefactoringTestSetup(someTest);
	}

	public RenamePackagePerfTests2(String name) {
		super(name);
	}
	
	public void testCold_10_10() throws Exception {
		executeRefactoring(generateSources(10, 10));
	}
	
	public void test_10_10() throws Exception {
		executeRefactoring(generateSources(10, 10));
	}
	
	public void test_10_100() throws Exception {
		executeRefactoring(generateSources(10, 100));
	}
	
	public void test_10_1000() throws Exception {
		executeRefactoring(generateSources(10, 1000));
	}
}
