/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.ui.tests.core;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

public class TypeHierarchyTest extends TestCase {
	
	private static final Class THIS= TypeHierarchyTest.class;
	
	private IJavaProject fJavaProject1;
	private IJavaProject fJavaProject2;

	public TypeHierarchyTest(String name) {
		super(name);
	}
			
	public static Test suite() {
		return new TestSuite(THIS);
	}
	
	protected void setUp() throws Exception {
		fJavaProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJavaProject2= JavaProjectHelper.createJavaProject("TestProject2", "bin");
	}

	protected void tearDown () throws Exception {
		JavaProjectHelper.delete(fJavaProject1);
		JavaProjectHelper.delete(fJavaProject2);		
	}
					
	public void test1() throws Exception {
		
		IPackageFragmentRoot jdk= JavaProjectHelper.addRTJar(fJavaProject1);
		assertTrue("jdk not found", jdk != null);
		IPackageFragmentRoot root1= JavaProjectHelper.addSourceContainer(fJavaProject1, "src");
		IPackageFragment pack1= root1.createPackageFragment("pack1", true, null);
		
		ICompilationUnit cu1= pack1.getCompilationUnit("A.java");
		cu1.createType("public class A {\n}\n", null, true, null);
		
		JavaProjectHelper.addRTJar(fJavaProject2);
		JavaProjectHelper.addRequiredProject(fJavaProject2, fJavaProject1);
		IPackageFragmentRoot root2= JavaProjectHelper.addSourceContainer(fJavaProject2, "src");
		IPackageFragment pack2= root2.createPackageFragment("pack2", true, null);
		
		ICompilationUnit cu2= pack2.getCompilationUnit("B.java");
		IType type2= cu2.createType("public class B extends pack1.A {\n}\n", null, true, null);
		
		ITypeHierarchy hierarchy= type2.newSupertypeHierarchy(null);
		IType[] allTypes= hierarchy.getAllTypes();
		
		assertTrue("Should contain 3 types, contains: " + allTypes.length, allTypes.length == 3);
		
		IType type= fJavaProject2.findType("pack1.A");
		assertTrue("Type not found", type != null);

	}	
	


}