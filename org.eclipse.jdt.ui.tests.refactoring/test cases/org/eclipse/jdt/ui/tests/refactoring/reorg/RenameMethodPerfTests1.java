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
import org.eclipse.jdt.core.IMethod;

import org.eclipse.jdt.internal.corext.refactoring.rename.RenameVirtualMethodProcessor;

import org.eclipse.jdt.ui.tests.refactoring.infra.AbstractRefactoringTestSetup;
import org.eclipse.jdt.ui.tests.refactoring.infra.RefactoringPerformanceTestCase;

import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;

public class RenameMethodPerfTests1 extends RefactoringPerformanceTestCase {

	private TestProject fTestProject;
	
	public static Test suite() {
		// we must make sure that cold is executed before warm
		TestSuite suite= new TestSuite("RenameTypePerfAcceptanceTests1");
		suite.addTest(new RenameMethodPerfTests1("testCold_10_10"));
		suite.addTest(new RenameMethodPerfTests1("test_10_10"));
		suite.addTest(new RenameMethodPerfTests1("test_100_10"));
		suite.addTest(new RenameMethodPerfTests1("test_1000_10"));
		return new AbstractRefactoringTestSetup(suite);
	}

	public static Test setUpTest(Test someTest) {
		return new AbstractRefactoringTestSetup(someTest);
	}

	public RenameMethodPerfTests1(String name) {
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
		// 100 referencing CUs each containing 10 references
		ICompilationUnit cunit= CodeGenerator.generateMethodRenameSources(fTestProject, 10, 10);
		executeRefactoring(cunit);
	}
	
	public void test_10_10() throws Exception {
		ICompilationUnit cunit= CodeGenerator.generateMethodRenameSources(fTestProject, 10, 10);
		executeRefactoring(cunit);
	}
	
	public void test_100_10() throws Exception {
		ICompilationUnit cunit= CodeGenerator.generateMethodRenameSources(fTestProject, 100, 10);
		executeRefactoring(cunit);
	}
	
	public void test_1000_10() throws Exception {
		ICompilationUnit cunit= CodeGenerator.generateMethodRenameSources(fTestProject, 1000, 10);
		executeRefactoring(cunit);
	}

	private void executeRefactoring(ICompilationUnit cunit) throws Exception {
		IMethod method= cunit.findPrimaryType().getMethod("foo", new String[0]);
		RenameVirtualMethodProcessor processor= new RenameVirtualMethodProcessor(method);
		processor.setNewElementName("foo2");
		executeRefactoring(new RenameRefactoring(processor));
	}
}
