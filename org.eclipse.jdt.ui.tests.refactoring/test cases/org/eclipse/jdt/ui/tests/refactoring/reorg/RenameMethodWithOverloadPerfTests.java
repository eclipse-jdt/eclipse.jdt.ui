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

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.corext.refactoring.rename.RenameVirtualMethodProcessor;

import org.eclipse.jdt.ui.tests.refactoring.infra.RefactoringPerformanceTestCase;
import org.eclipse.jdt.ui.tests.refactoring.infra.RefactoringPerformanceTestSetup;

import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;

public class RenameMethodWithOverloadPerfTests extends RefactoringPerformanceTestCase {

	private TestProject fTestProject;
	
	public static Test suite() {
		// we must make sure that cold is executed before warm
		TestSuite suite= new TestSuite("RenameMethodWithOverloadPerfTests");
		suite.addTest(new RenameMethodWithOverloadPerfTests("testCold_10_10"));
		suite.addTest(new RenameMethodWithOverloadPerfTests("test_10_10"));
		suite.addTest(new RenameMethodWithOverloadPerfTests("test_100_10"));
		suite.addTest(new RenameMethodWithOverloadPerfTests("test_1000_10"));
		return new RefactoringPerformanceTestSetup(suite);
	}

	public static Test setUpTest(Test someTest) {
		return new RefactoringPerformanceTestSetup(someTest);
	}

	public RenameMethodWithOverloadPerfTests(String name) {
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
		executeRefactoring(generateSources(fTestProject, 10, 10));
	}
	
	public void test_10_10() throws Exception {
		executeRefactoring(generateSources(fTestProject, 10, 10));
	}
	
	public void test_100_10() throws Exception {
		tagAsSummary("Rename method with overloading", Dimension.CPU_TIME);
		executeRefactoring(generateSources(fTestProject, 100, 10));
	}
	
	public void test_1000_10() throws Exception {
		executeRefactoring(generateSources(fTestProject, 1000, 10));
	}

	private void executeRefactoring(ICompilationUnit cunit) throws Exception {
		IMethod method= cunit.findPrimaryType().getMethod("setString", new String[] {"QString;"});
		RenameVirtualMethodProcessor processor= new RenameVirtualMethodProcessor(method);
		processor.setNewElementName("set");
		executeRefactoring(new RenameRefactoring(processor));
	}
	
	private static ICompilationUnit generateSources(TestProject testProject, int numberOfCus, int numberOfRefs) throws Exception {
		IPackageFragment definition= testProject.getSourceFolder().createPackageFragment("def", false, null); 
		StringBuffer buf= new StringBuffer();
		buf.append("package def;\n");
		buf.append("public class A {\n");
		buf.append("    public void set(Object s) {\n");
		buf.append("    }\n");
		buf.append("    public void setString(String s) {\n");
		buf.append("    }\n");
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
		buf.append("    public void ref(A a) {\n");
		buf.append("        String s= \"Eclipse\");\n");
		for (int i= 0; i < numberOfRefs; i++) {
			buf.append("        a.set(s);\n");
			buf.append("        a.setString(s);\n");
		}
		buf.append("    }\n");
		buf.append("}\n");
		pack.createCompilationUnit("Ref" + index + ".java", buf.toString(), false, null);
	}	
}
