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

import org.eclipse.jdt.ui.tests.refactoring.infra.RefactoringPerformanceTestSetup;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;

public class RenameMethodWithHidingPerfTests extends RepeatingRefactoringPerformanceTestCase {

	public static Test suite() {
		// we must make sure that cold is executed before warm
		TestSuite suite= new TestSuite("RenameMethodWithHidingPerfTests");
		suite.addTest(new RenameMethodWithHidingPerfTests("testCold"));
		suite.addTest(new RenameMethodWithHidingPerfTests("testWarm"));
		return new RefactoringPerformanceTestSetup(suite);
	}

	public static Test setUpTest(Test someTest) {
		return new RefactoringPerformanceTestSetup(someTest);
	}

	public RenameMethodWithHidingPerfTests(String name) {
		super(name);
	}
	
	public void testCold() throws Exception {
		executeRefactoring(0, 0, false, 10);
	}
	
	public void testWarm() throws Exception {
		tagAsSummary("Rename method with hiding", Dimension.CPU_TIME);
		executeRefactoring(0, 0, true, 10);
	}
	
	protected void doExecuteRefactoring(int numberOfCus, int numberOfRefs, boolean measure) throws Exception {
		ICompilationUnit cunit= generateSources(fTestProject);
		IMethod method= cunit.findPrimaryType().getMethod("setString", new String[] {"QString;"});
		RenameVirtualMethodProcessor processor= new RenameVirtualMethodProcessor(method);
		processor.setNewElementName("set");
		executeRefactoring(new RenameRefactoring(processor), measure, RefactoringStatus.ERROR);
	}
	
	private ICompilationUnit generateSources(TestProject testProject) throws Exception {
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
