/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.testplugin.test;

import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.testplugin.JavaTestProject;
import org.eclipse.jdt.testplugin.JavaTestSetup;
import org.eclipse.jdt.testplugin.TestPluginLauncher;
import org.eclipse.jdt.testplugin.TestPluginLauncher;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.jdt.testplugin.*;


public class JavaTestCase extends TestCase {
	
	private JavaTestProject fTestProject;

	public JavaTestCase(String name) {
		super(name);
	}

	public static void main(String[] args) {
		TestPluginLauncher.run(TestPluginLauncher.getLocationFromProperties(), JavaTestCase.class, args);
	}		
			
	public static Test suite() {
		TestSuite suite= new TestSuite();
		suite.addTest(new JavaTestCase("doTest1"));
		return new JavaTestSetup(suite);
	}
	
	/*
	 * create a new source container "src"
	 */	
	protected void setUp() throws Exception {
		fTestProject= JavaTestSetup.getTestProject();

		IPackageFragmentRoot root= fTestProject.addSourceContainer("src");
		IPackageFragment pack= root.createPackageFragment("ibm.util", true, null);
		
		ICompilationUnit cu= pack.getCompilationUnit("A.java");
		IType type= cu.createType("public class A {\n}\n", null, true, null);
		type.createMethod("public void a() {}\n", null, true, null);
		type.createMethod("public void b(java.util.Vector v) {}\n", null, true, null);
	}

	/*
	 * remove the source container
	 */	
	protected void tearDown () throws Exception {
		fTestProject.removeSourceContainer("src");
	}
				
	/*
	 * basic test: check for created methods
	 */
	public void doTest1() throws Exception {
		IJavaProject jproject= fTestProject.getJavaProject();
		
		String name= "ibm/util/A.java";
		ICompilationUnit cu= (ICompilationUnit)jproject.findElement(new Path(name));
		assert("A.java must exist", cu != null);
		IType type= cu.getType("A");
		assert("Type A must exist", type != null);
		
		System.out.println("methods of A");
		IMethod[] methods= type.getMethods();
		for (int i= 0; i < methods.length; i++) {
			System.out.println(methods[i].getElementName());
		}
		assert("Should contain 2 methods", methods.length == 2);
	}	
	

}