/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.ui.tests.core;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IWorkspaceRoot;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;

import org.eclipse.jdt.testplugin.JavaTestPlugin;
import org.eclipse.jdt.testplugin.JavaTestProject;
import org.eclipse.jdt.testplugin.JavaTestSetup;
import org.eclipse.jdt.testplugin.TestPluginLauncher;
import org.eclipse.jdt.testplugin.TestPluginLauncher;

import org.eclipse.jdt.internal.ui.util.JavaModelUtility;
import org.eclipse.jdt.testplugin.*;

public class TypeHierarchyTest extends TestCase {
	
	private JavaTestProject fTestProject;

	public TypeHierarchyTest(String name) {
		super(name);
	}

	public static void main(String[] args) {
		TestPluginLauncher.run(TestPluginLauncher.getLocationFromProperties(), TypeHierarchyTest.class, args);
	}		
			
	public static Test suite() {
		TestSuite suite= new TestSuite();
		suite.addTest(new TypeHierarchyTest("doTest1"));
		return new JavaTestSetup(suite);
	}
	
	protected void setUp() throws Exception {
	}

	protected void tearDown () throws Exception {
	}
					
	public void doTest1() throws Exception {
		IWorkspaceRoot workspaceRoot= JavaTestPlugin.getWorkspace().getRoot();
		
		JavaTestProject proj1= new JavaTestProject(workspaceRoot, "TestProject1", "bin");
		IPackageFragmentRoot jdk= proj1.addRTJar();
		assert("jdk not found", jdk != null);
		IPackageFragmentRoot root1= proj1.addSourceContainer("src");
		IPackageFragment pack1= root1.createPackageFragment("pack1", true, null);
		
		ICompilationUnit cu1= pack1.getCompilationUnit("A.java");
		IType type1= cu1.createType("public class A {\n}\n", null, true, null);
			
		JavaTestProject proj2= new JavaTestProject(workspaceRoot, "TestProject2", "bin");
		proj2.addRTJar();
		proj2.addRequiredProject(proj1.getJavaProject());
		IPackageFragmentRoot root2= proj2.addSourceContainer("src");
		IPackageFragment pack2= root2.createPackageFragment("pack2", true, null);
		
		ICompilationUnit cu2= pack2.getCompilationUnit("B.java");
		IType type2= cu2.createType("public class B extends pack1.A {\n}\n", null, true, null);
		
		ITypeHierarchy hierarchy= type2.newSupertypeHierarchy(null);
		IType[] allTypes= hierarchy.getAllTypes();
		
		System.out.println("all types in TH of B");
		for (int i= 0; i < allTypes.length; i++) {
			System.out.print(allTypes[i].getElementName());
			System.out.print(" - ");
			System.out.println(allTypes[i].getJavaProject().getElementName());
		}
		assert("Should contain 3 types, contains: " + allTypes.length, allTypes.length == 3);
		
		IType type= JavaModelUtility.findType(proj2.getJavaProject(), "pack1.A");
		assert("Type not found", type != null);
		System.out.println("Using findElement");
		System.out.print(type.getElementName());
		System.out.print(" - ");
		System.out.println(type.getJavaProject().getElementName());
		
		
		proj2.remove();
		proj1.remove();
	}	
	

}