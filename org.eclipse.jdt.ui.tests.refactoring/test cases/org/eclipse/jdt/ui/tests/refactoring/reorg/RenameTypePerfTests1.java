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
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.corext.refactoring.rename.RenameTypeProcessor;

import org.eclipse.jdt.ui.tests.refactoring.infra.AbstractRefactoringTestSetup;
import org.eclipse.jdt.ui.tests.refactoring.infra.RefactoringPerformanceTestCase;

import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;

public class RenameTypePerfTests1 extends RefactoringPerformanceTestCase {

	private TestProject fTestProject;
	
	public static Test suite() {
		// we must make sure that cold is executed before warm
		TestSuite suite= new TestSuite("RenameTypePerfTests1");
		suite.addTest(new RenameTypePerfTests1("testCold_10_10"));
		suite.addTest(new RenameTypePerfTests1("test_10_10"));
		suite.addTest(new RenameTypePerfTests1("test_100_10"));
		suite.addTest(new RenameTypePerfTests1("test_1000_10"));
		return new AbstractRefactoringTestSetup(suite);
	}

	public static Test setUpTest(Test someTest) {
		return new AbstractRefactoringTestSetup(someTest);
	}

	public RenameTypePerfTests1(String name) {
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
		executeRefactoring(generateSources(fTestProject, 10, 10));
	}
	
	public void test_10_10() throws Exception {
		executeRefactoring(generateSources(fTestProject, 10, 10));
	}
	
	public void test_100_10() throws Exception {
		executeRefactoring(generateSources(fTestProject, 100, 10));
	}
	
	public void test_1000_10() throws Exception {
		executeRefactoring(generateSources(fTestProject, 1000, 10));
	}

	private void executeRefactoring(ICompilationUnit cunit) throws Exception {
		IType type= cunit.findPrimaryType();
		RenameTypeProcessor processor= new RenameTypeProcessor(type);
		processor.setNewElementName("B");
		executeRefactoring(new RenameRefactoring(processor));
	}

	public static ICompilationUnit generateSources(TestProject testProject, int numberOfCus, int numberOfRefs) throws Exception {
		IPackageFragment definition= testProject.getSourceFolder().createPackageFragment("def", false, null); 
		StringBuffer buf= new StringBuffer();
		buf.append("package def;\n");
		buf.append("public class A {\n");
		buf.append("}\n");
		ICompilationUnit result= definition.createCompilationUnit("A.java", buf.toString(), false, null);
	
		IPackageFragment references= testProject.getSourceFolder().createPackageFragment("ref", false, null);
		for(int i= 0; i < numberOfCus; i++) {
			createReferenceCu(references, i, numberOfRefs);
		}
		return result;
	}

	private static void createReferenceCu(IPackageFragment pack, int index, int numberOfRefs) throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package " + pack.getElementName() + ";\n");
		buf.append("import def.A;\n");
		buf.append("public class Ref" + index + " {\n");
		for (int i= 0; i < numberOfRefs - 1; i++) {
			buf.append("    A field" + i +";\n");
		}
		buf.append("}\n");
		pack.createCompilationUnit("Ref" + index + ".java", buf.toString(), false, null);
	}
}
