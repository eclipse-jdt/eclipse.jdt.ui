/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;


public class AddUnimplementedConstructorsTest extends TestCase {
	private static final Class THIS= AddUnimplementedConstructorsTest.class;
	
	private IJavaProject fJavaProject;
	private IPackageFragment fPackage;
	private IType fClassA;

	public AddUnimplementedConstructorsTest(String name) {
		super(name);
	}
	
	
	public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(THIS));
	}

	public static Test suite() {
		if (true) {
			return allTests();
		} else {
			TestSuite suite= new TestSuite();
			suite.addTest(new AddUnimplementedMethodsTest("test1"));
			return suite;
		}	
	}

	/**
	 * Creates a new test Java project.
	 */	
	protected void setUp() throws Exception {
	}	
	
	/**
	 * Removes the test java project.
	 */
	protected void tearDown () throws Exception {
		JavaProjectHelper.delete(fJavaProject);
		fJavaProject= null;
		fPackage= null;
		fClassA= null;
	}
	
	/*
	 * basic test: test with one constructor
	 */
	public void test1() throws Exception {	
		fJavaProject= JavaProjectHelper.createJavaProject("DummyProject", "bin");
		assertNotNull(JavaProjectHelper.addRTJar(fJavaProject));
		
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fJavaProject, "src");
		fPackage= root.createPackageFragment("ibm.util", true, null);
		
		ICompilationUnit cu= fPackage.getCompilationUnit("A.java");
		fClassA= cu.createType("public class A {\n}\n", null, true, null);		
		fClassA.createMethod("public A() {\nsuper();}\n", null, true, null);
		
		ICompilationUnit cu2= fPackage.getCompilationUnit("Test1.java"); //$NON-NLS-1$
		IType testClass= cu2.createType("public class Test1 extends A {\n}\n", null, true, null); //$NON-NLS-1$
	
		IMethod[] constructorMethods= StubUtility.getOverridableConstructors(testClass);
		
		checkMethods(new String[] { "A" }, constructorMethods); //$NON-NLS-1$
	}
	
	/*
	 * basic test: test with 2 constructors to override
	 */
	public void test2() throws Exception {	
		fJavaProject= JavaProjectHelper.createJavaProject("DummyProject", "bin");
		assertNotNull(JavaProjectHelper.addRTJar(fJavaProject));
		
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fJavaProject, "src");
		fPackage= root.createPackageFragment("ibm.util", true, null);
		
		ICompilationUnit cu= fPackage.getCompilationUnit("A.java");
		fClassA= cu.createType("public class A {\n}\n", null, true, null);		
		fClassA.createMethod("public A() {\nsuper();}\n", null, true, null);
		fClassA.createMethod("public A(int a) {super()}\n", null, true, null);
		
		ICompilationUnit cu2= fPackage.getCompilationUnit("Test1.java");
		IType testClass= cu2.createType("public class Test1 extends A {\n}\n", null, true, null);
	
		IMethod[] constructorMethods= StubUtility.getOverridableConstructors(testClass);
		
		checkMethods(new String[] { "A", "A" }, constructorMethods);	 //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	/*
	 * basic test: test with 3 constructors to override
	 */
	public void test3() throws Exception {	
		fJavaProject= JavaProjectHelper.createJavaProject("DummyProject", "bin");
		assertNotNull(JavaProjectHelper.addRTJar(fJavaProject));
		
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fJavaProject, "src");
		fPackage= root.createPackageFragment("ibm.util", true, null);
		
		ICompilationUnit cu= fPackage.getCompilationUnit("A.java");
		fClassA= cu.createType("public class A {\n}\n", null, true, null);		
		fClassA.createMethod("public A() {\nsuper();}\n", null, true, null);
		fClassA.createMethod("public A(int a) {super()}\n", null, true, null);
		fClassA.createMethod("public A(int a, boolean boo) {super()}\n", null, true, null);
		
		ICompilationUnit cu2= fPackage.getCompilationUnit("Test1.java");
		IType testClass= cu2.createType("public class Test1 extends A {\n}\n", null, true, null);
	
		IMethod[] constructorMethods= StubUtility.getOverridableConstructors(testClass);
		
		checkMethods(new String[] { "A", "A", "A" }, constructorMethods);	 //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	/*
	 * basic test: test with default constructor only
	 */
	public void test4() throws Exception {	
		fJavaProject= JavaProjectHelper.createJavaProject("DummyProject", "bin");
		assertNotNull(JavaProjectHelper.addRTJar(fJavaProject));
		
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fJavaProject, "src");
		fPackage= root.createPackageFragment("ibm.util", true, null);
		
		ICompilationUnit cu= fPackage.getCompilationUnit("A.java");
		fClassA= cu.createType("public class A {\n}\n", null, true, null);		
		
		ICompilationUnit cu2= fPackage.getCompilationUnit("Test1.java");
		IType testClass= cu2.createType("public class Test1 extends A {\n}\n", null, true, null);
	
		IMethod[] constructorMethods= StubUtility.getOverridableConstructors(testClass);
		
		checkMethods(new String[] { "Object" }, constructorMethods);	 //$NON-NLS-1$ //$NON-NLS-2$
	}	
	
	/*
	 * basic test: test with nothing to override
	 */
	public void test5() throws Exception {	
		fJavaProject= JavaProjectHelper.createJavaProject("DummyProject", "bin");
		assertNotNull(JavaProjectHelper.addRTJar(fJavaProject));
		
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fJavaProject, "src");
		fPackage= root.createPackageFragment("ibm.util", true, null);
		
		ICompilationUnit cu= fPackage.getCompilationUnit("A.java");
		fClassA= cu.createType("public class A {\n}\n", null, true, null);		
		
		ICompilationUnit cu2= fPackage.getCompilationUnit("Test1.java");
		IType testClass= cu2.createType("public class Test1 extends A {\n}\n", null, true, null);
		testClass.createMethod("public Test1(){}\n", null, true, null);
	
		IMethod[] constructorMethods= StubUtility.getOverridableConstructors(testClass);
		
		checkMethods(new String[] {}, constructorMethods);	 //$NON-NLS-1$ //$NON-NLS-2$
	}	

	private void checkMethods(String[] expected, IMethod[] methods) {
		int nMethods= methods.length;
		int nExpected= expected.length;
		assertTrue("" + nExpected + " methods expected, is " + nMethods, nMethods == nExpected);
		for (int i= 0; i < nExpected; i++) {
			String methName= expected[i];
			assertTrue("method " + methName + " expected", nameContained(methName, methods));
		}
	}			
		
	private boolean nameContained(String methName, IJavaElement[] methods) {
		for (int i= 0; i < methods.length; i++) {
			if (methods[i].getElementName().equals(methName)) {
				return true;
			}
		}
		return false;
	}			
			
}
