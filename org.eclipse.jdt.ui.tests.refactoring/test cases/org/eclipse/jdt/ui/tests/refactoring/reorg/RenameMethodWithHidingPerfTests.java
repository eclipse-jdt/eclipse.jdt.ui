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
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.corext.refactoring.rename.RenameVirtualMethodProcessor;

import org.eclipse.jdt.ui.tests.refactoring.infra.AbstractRefactoringTestSetup;
import org.eclipse.jdt.ui.tests.refactoring.infra.RefactoringPerformanceTestCase;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;

public class RenameMethodWithHidingPerfTests extends RefactoringPerformanceTestCase {

	private TestProject fTestProject;
	
	public static Test suite() {
		// we must make sure that cold is executed before warm
		TestSuite suite= new TestSuite("RenameMethodWithHidingPerfTests");
		suite.addTest(new RenameMethodWithHidingPerfTests("testCold_10_10"));
		suite.addTest(new RenameMethodWithHidingPerfTests("test_10_10"));
		return new AbstractRefactoringTestSetup(suite);
	}

	public static Test setUpTest(Test someTest) {
		return new AbstractRefactoringTestSetup(someTest);
	}

	public RenameMethodWithHidingPerfTests(String name) {
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
		executeRefactoring(generateSources(fTestProject));
	}
	
	public void test_10_10() throws Exception {
		executeRefactoring(generateSources(fTestProject));
	}
	
	private void executeRefactoring(ICompilationUnit cunit) throws Exception {
		IMethod method= cunit.findPrimaryType().getMethod("setString", new String[] {"QString;"});
		RenameVirtualMethodProcessor processor= new RenameVirtualMethodProcessor(method);
		processor.setNewElementName("set");
		executeRefactoring(new RenameRefactoring(processor), RefactoringStatus.ERROR);
	}
	
	private static ICompilationUnit generateSources(TestProject testProject) throws Exception {
		IPackageFragment definition= testProject.getSourceFolder().createPackageFragment("def", false, null); 
		StringBuffer buf= new StringBuffer();
		buf.append("package def;\n");
		buf.append("public class A {\n");
		buf.append("    public void setString(String s) {\n");
		buf.append("    }\n");
		buf.append("    public class Defines {\n");
		buf.append("        public void set(String s) {}\n");
		buf.append("    }\n");
		buf.append("    public class Ref extends Defines {\n");
		buf.append("        public void ref() {\n");
		buf.append("            setString(\"Eclipse\");\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		return definition.createCompilationUnit("A.java", buf.toString(), false, null);
	}
}
