/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.testplugin.test;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.testplugin.JavaTestPlugin;
import org.eclipse.jdt.testplugin.JavaTestProject;
import org.eclipse.jdt.testplugin.TestPluginLauncher;
import org.eclipse.jdt.testplugin.TestPluginLauncher;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.jdt.testplugin.*;


public class HelloWorld extends TestCase {
	
	public static void main(String[] args) {
		TestPluginLauncher.run(TestPluginLauncher.getLocationFromProperties(), HelloWorld.class, args);
	}
	
	public static Test suite() {
		TestSuite suite= new TestSuite();
		suite.addTest(new HelloWorld("test1"));
		return suite;
	}		
	
	public HelloWorld(String name) {
		super(name);
	}
		
	public void test1() throws Exception {
		IWorkspaceRoot root= JavaTestPlugin.getWorkspace().getRoot();
		JavaTestProject testProject= new JavaTestProject(root, "TestProject", "bin");
		
		if (testProject.addRTJar() == null) {
			assert("jdk not found", false);
			return;
		}
		
		IJavaProject jproject= testProject.getJavaProject();
		
		String name= "java/util/Vector.java";
		IClassFile classfile= (IClassFile)jproject.findElement(new Path(name));
		assert("classfile not found", classfile != null);
		
		IType type= classfile.getType();
		System.out.println("methods of Vector");
		IMethod[] methods= type.getMethods();
		for (int i= 0; i < methods.length; i++) {
			System.out.println(methods[i].getElementName());
		}
	}		

}