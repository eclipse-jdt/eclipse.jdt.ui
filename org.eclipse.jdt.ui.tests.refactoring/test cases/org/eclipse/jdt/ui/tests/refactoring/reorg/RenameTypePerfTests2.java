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

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.corext.refactoring.rename.RenameTypeProcessor;

import org.eclipse.jdt.ui.tests.refactoring.infra.AbstractRefactoringTestSetup;
import org.eclipse.jdt.ui.tests.refactoring.infra.RefactoringPerformanceTestCase;

import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;

public class RenameTypePerfTests2 extends RefactoringPerformanceTestCase {

	private TestProject fTestProject;
	
	public static Test suite() {
		// we must make sure that cold is executed before warm
		TestSuite suite= new TestSuite("RenameTypePerfTests2");
		suite.addTest(new RenameTypePerfTests2("testCold_10_10"));
		suite.addTest(new RenameTypePerfTests2("test_10_10"));
		suite.addTest(new RenameTypePerfTests2("test_10_100"));
		suite.addTest(new RenameTypePerfTests2("test_10_1000"));
		return new AbstractRefactoringTestSetup(suite);
	}

	public static Test setUpTest(Test someTest) {
		return new AbstractRefactoringTestSetup(someTest);
	}

	public RenameTypePerfTests2(String name) {
		super(name);
	}
	
	protected void setUp() throws Exception {
		super.setUp();
		fTestProject= new TestProject();
	}
	
	protected void tearDown() throws Exception {
		fTestProject.delete();
		super.tearDown();
	}
	
	public void testCold_10_10() throws Exception {
		executeRefactoring(RenameTypePerfTests1.generateSources(fTestProject, 10, 10));
	}
	
	public void test_10_10() throws Exception {
		executeRefactoring(RenameTypePerfTests1.generateSources(fTestProject, 10, 10));
	}
	
	public void test_10_100() throws Exception {
		executeRefactoring(RenameTypePerfTests1.generateSources(fTestProject, 10, 100));
	}
	
	public void test_10_1000() throws Exception {
		executeRefactoring(RenameTypePerfTests1.generateSources(fTestProject, 10, 1000));
	}

	private void executeRefactoring(ICompilationUnit cunit) throws Exception {
		IType type= cunit.findPrimaryType();
		RenameTypeProcessor processor= new RenameTypeProcessor(type);
		processor.setNewElementName("B");
		executeRefactoring(new RenameRefactoring(processor));
	}
}
