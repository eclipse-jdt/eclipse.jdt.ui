/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.refactoring.tests;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.internal.core.refactoring.RenameResourceChange;
import org.eclipse.jdt.testplugin.JavaTestSetup;
import org.eclipse.jdt.testplugin.TestPluginLauncher;

public class PathTransformationTests extends TestCase {

	public PathTransformationTests(String name) {
		super(name);
	}
	
	public static void main(String[] args) {
		args= new String[] { PathTransformationTests.class.getName() };
		TestPluginLauncher.runUI(TestPluginLauncher.getLocationFromProperties(), args);
	}
	
	public static Test suite() {
		TestSuite suite= new TestSuite();
		suite.addTest(noSetupSuite());
		return new JavaTestSetup(suite);
	}
	
	public static Test noSetupSuite() {
		return new TestSuite(PathTransformationTests.class);
	}
	
/************/	
	private IPath createIPath(String p){
		return Path.EMPTY.append(p);
	}
	
	private void  check(String path, String oldName, String newName){
		IPath pOld= createIPath(path + "/" + oldName);
		String extension= "";
		if (oldName.lastIndexOf(".") != -1)
			extension= oldName.substring(oldName.lastIndexOf("."));
		IPath pNew= createIPath(path + "/" + newName + extension);
		IPath newPath= RenameResourceChange.renamedResourcePath(pOld, newName);
		
		assertEquals(pNew.toString(), newPath.toString());
	}
	
/************/
	
	public void test0(){
		check("/s/p", "A.java", "B");
	}
	
	public void test1(){
		check("/s/p", "A.java", "A");
	}		

	public void test2(){
		check("/s/p", "A.txt", "B");
	}		
	
	public void test3(){
		check("/s/p", "A", "B");
	}		
	
	public void test4(){
		check("/s/p/p", "A.java", "B");
	}
	
	public void test5(){
		check("/s/p/p", "A.java", "A");
	}		

	public void test6(){
		check("/s/p/p", "A.txt", "B");
	}		
	
	public void test7(){
		check("/s", "A", "B");
	}		
	
	public void test8(){
		check("/s", "A.java", "B");
	}
	
	public void test9(){
		check("/s", "A.java", "A");
	}		

	public void test10(){
		check("/s", "A.txt", "B");
	}		
	
	public void test11(){
		check("/s", "A", "B");
	}			
	
	public void test12(){
		check("", "A.java", "B");
	}
	
	public void test13(){
		check("", "A.java", "A");
	}		

	public void test14(){
		check("", "A.txt", "B");
	}		
	
	public void test15(){
		check("", "A", "B");
	}			
}