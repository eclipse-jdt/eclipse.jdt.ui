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
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.corext.refactoring.rename.RenameVirtualMethodProcessor;
import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.ui.tests.refactoring.infra.AbstractRefactoringTestSetup;
import org.eclipse.jdt.ui.tests.refactoring.infra.RefactoringPerformanceTestCase;

import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;

public class RenameMethodPerfTests extends RefactoringPerformanceTestCase {
	
	private IJavaProject fTestProject;
	private IPackageFragmentRoot fSourceFolder;
	private ICompilationUnit fCUnit;
	
	public static Test suite() {
		return new AbstractRefactoringTestSetup(new TestSuite(RenameMethodPerfTests.class));
	}

	public static Test setUpTest(Test someTest) {
		return new AbstractRefactoringTestSetup(someTest);
	}

	protected void setUp() throws Exception {
		super.setUp();
		fTestProject= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		assertTrue("rt not found", JavaProjectHelper.addRTJar(fTestProject) != null);
		fSourceFolder= JavaProjectHelper.addSourceContainer(fTestProject, "src");
		fCUnit= generateTestSources();
	}
	
	protected void tearDown() throws Exception {
		if (fTestProject != null && fTestProject.exists())
			JavaProjectHelper.delete(fTestProject);
		super.tearDown();
	}
	
	public void testRenameMethod() throws Exception {
		IMethod method= fCUnit.findPrimaryType().getMethod("foo", new String[0]);
		RenameVirtualMethodProcessor processor= new RenameVirtualMethodProcessor(method);
		processor.setNewElementName("foo2");
		executeRefactoring(new RenameRefactoring(processor), "cold");
		
		method= fCUnit.findPrimaryType().getMethod("foo2", new String[0]);
		processor= new RenameVirtualMethodProcessor(method);
		processor.setNewElementName("foo");
		executeRefactoring(new RenameRefactoring(processor), "warm");
	}
	
	private ICompilationUnit generateTestSources() throws Exception {
		IPackageFragment definition= fSourceFolder.createPackageFragment("def", false, null); 
		StringBuffer buf= new StringBuffer();
		buf.append("package def;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit result= definition.createCompilationUnit("A.java", buf.toString(), false, null);

		IPackageFragment references= fSourceFolder.createPackageFragment("ref", false, null);
		for(int i= 0; i < 100; i++) {
			createReferenceCu(references, i);
		}
		return result;
	}
	
	private void createReferenceCu(IPackageFragment pack, int index) throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package " + pack.getElementName() + ";\n");
		buf.append("import def.A;\n");
		buf.append("public class Ref" + index + " {\n");
		buf.append("    public void ref(A a) {\n");
		for (int i= 0; i < 10; i++) {
			buf.append("        a.foo();\n");
		}
		buf.append("    }\n");
		buf.append("}\n");
		pack.createCompilationUnit("Ref" + index + ".java", buf.toString(), false, null);
	}
}
