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

import java.util.ArrayList;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.internal.corext.codemanipulation.AddUnimplementedMethodsOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.util.JdtFlags;

public class AddUnimplementedMethodsTest extends TestCase {
	
	private static final Class THIS= AddUnimplementedMethodsTest.class;
	
	private IJavaProject fJavaProject;
	private IPackageFragment fPackage;
	private IType fClassA, fInterfaceB, fClassC, fClassD, fInterfaceE;


	public AddUnimplementedMethodsTest(String name) {
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
			return new ProjectTestSetup(suite);
		}	
	}
	
	/**
	 * Creates a new test Java project.
	 */	
	protected void setUp() throws Exception {
		fJavaProject= JavaProjectHelper.createJavaProject("DummyProject", "bin");
		assertNotNull(JavaProjectHelper.addRTJar(fJavaProject));
		
		IPackageFragmentRoot root= JavaProjectHelper.addSourceContainer(fJavaProject, "src");
		fPackage= root.createPackageFragment("ibm.util", true, null);
		
		ICompilationUnit cu= fPackage.getCompilationUnit("A.java");
		fClassA= cu.createType("public abstract class A {\n}\n", null, true, null);
		fClassA.createMethod("public abstract void a();\n", null, true, null);
		fClassA.createMethod("public abstract void b(java.util.Vector v);\n", null, true, null);
		
		cu= fPackage.getCompilationUnit("B.java");
		fInterfaceB= cu.createType("public interface B {\n}\n", null, true, null);
		fInterfaceB.createMethod("void c(java.util.Hashtable h);\n", null, true, null);
		
		cu= fPackage.getCompilationUnit("C.java");
		fClassC= cu.createType("public abstract class C {\n}\n", null, true, null);
		fClassC.createMethod("public void c(java.util.Hashtable h) {\n}\n", null, true, null);
		fClassC.createMethod("public abstract java.util.Enumeration d(java.util.Hashtable h) {\n}\n", null, true, null);


		cu= fPackage.getCompilationUnit("D.java");
		fClassD= cu.createType("public abstract class D extends C {\n}\n", null, true, null);
		fClassD.createMethod("public abstract void c(java.util.Hashtable h);\n", null, true, null);
		
		cu= fPackage.getCompilationUnit("E.java");
		fInterfaceE= cu.createType("public interface E {\n}\n", null, true, null);
		fInterfaceE.createMethod("void c(java.util.Hashtable h);\n", null, true, null);
		fInterfaceE.createMethod("void e() throws java.util.NoSuchElementException;\n", null, true, null);	
	}


	/**
	 * Removes the test java project.
	 */
	protected void tearDown () throws Exception {
		JavaProjectHelper.delete(fJavaProject);
		fJavaProject= null;
		fPackage= null;
		fClassA= null;
		fInterfaceB= null;
		fClassC= null;
		fClassD= null;
		fInterfaceE= null;
	}
	
	private IMethod[] getSelected(IType type) throws JavaModelException {
		IMethod[] inheritedMethods= StubUtility.getOverridableMethods(type, type.newSupertypeHierarchy(null), false);
		
		ArrayList toImplement= new ArrayList();
		for (int i= 0; i < inheritedMethods.length; i++) {
			IMethod curr= inheritedMethods[i];
			if (JdtFlags.isAbstract(curr)) {
				toImplement.add(curr);
			}
		}
		return (IMethod[]) toImplement.toArray(new IMethod[toImplement.size()]);
	}
	
				
	/*
	 * basic test: extend an abstract class and an interface
	 */
	public void test1() throws Exception {	
		ICompilationUnit cu= fPackage.getCompilationUnit("Test1.java");
		IType testClass= cu.createType("public class Test1 extends A implements B {\n}\n", null, true, null);
	
		AddUnimplementedMethodsOperation op= new AddUnimplementedMethodsOperation(testClass, new CodeGenerationSettings(), getSelected(testClass), true, null);
		op.run(new NullProgressMonitor());
		
		IMethod[] methods= testClass.getMethods();
		checkMethods(new String[] { "a", "b", "c" }, methods);
		
		IImportDeclaration[] imports= cu.getImports();
		checkImports(new String[] { "java.util.Hashtable", "java.util.Vector" }, imports);	
	}	
	
	/*
	 * method c() of interface B is already implemented by class C
	 */
	public void test2() throws Exception {
			
		ICompilationUnit cu= fPackage.getCompilationUnit("Test2.java");
		IType testClass= cu.createType("public class Test2 extends C implements B {\n}\n", null, true, null);
		
		AddUnimplementedMethodsOperation op= new AddUnimplementedMethodsOperation(testClass, new CodeGenerationSettings(), getSelected(testClass), true, null);
		op.run(new NullProgressMonitor());
		
		IMethod[] methods= testClass.getMethods();
		checkMethods(new String[] { "d" }, methods);
		
		IImportDeclaration[] imports= cu.getImports();
		checkImports(new String[] { "java.util.Enumeration", "java.util.Hashtable" }, imports);
	}	


	/*
	 * method c() is implemented in C but made abstract again in class D
	 */
	public void test3() throws Exception {
		ICompilationUnit cu= fPackage.getCompilationUnit("Test3.java");
		IType testClass= cu.createType("public class Test3 extends D {\n}\n", null, true, null);
		
		AddUnimplementedMethodsOperation op= new AddUnimplementedMethodsOperation(testClass, new CodeGenerationSettings(), getSelected(testClass), true, null);
		op.run(new NullProgressMonitor());
		
		IMethod[] methods= testClass.getMethods();
		checkMethods(new String[] { "c", "d" }, methods);
		
		IImportDeclaration[] imports= cu.getImports();
		checkImports(new String[] { "java.util.Hashtable", "java.util.Enumeration" }, imports);
		
	}
	
	/*
	 * method c() defined in both interfaces B and E
	 */
	public void test4() throws Exception {
		ICompilationUnit cu= fPackage.getCompilationUnit("Test4.java");
		IType testClass= cu.createType("public class Test4 implements B, E {\n}\n", null, true, null);
		
		AddUnimplementedMethodsOperation op= new AddUnimplementedMethodsOperation(testClass, new CodeGenerationSettings(), getSelected(testClass), true, null);
		op.run(new NullProgressMonitor());
		
		IMethod[] methods= testClass.getMethods();
		checkMethods(new String[] { "c", "e" }, methods);
		
		IImportDeclaration[] imports= cu.getImports();
		checkImports(new String[] { "java.util.Hashtable", "java.util.NoSuchElementException" }, imports);
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
	
	private void checkImports(String[] expected, IImportDeclaration[] imports) {
		int nImports= imports.length;
		int nExpected= expected.length;
		assertTrue("" + nExpected + " imports expected, is " + nImports, nImports == nExpected);
		for (int i= 0; i < nExpected; i++) {
			String impName= expected[i];
			assertTrue("import " + impName + " expected", nameContained(impName, imports));
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
