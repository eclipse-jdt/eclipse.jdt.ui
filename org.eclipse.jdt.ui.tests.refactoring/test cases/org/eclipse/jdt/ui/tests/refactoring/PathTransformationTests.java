/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.ui.tests.refactoring;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.internal.core.refactoring.changes.RenameResourceChange;

public class PathTransformationTests extends TestCase {
	
	private static final Class clazz= PathTransformationTests.class;
	public PathTransformationTests(String name) {
		super(name);
	}
	
	public static Test suite() {
		TestSuite suite= new TestSuite(clazz.getName());
		suite.addTest(noSetupSuite());
		return new MySetup(suite);
	}
	
	public static Test noSetupSuite() {
		return new TestSuite(clazz);
	}
	
/************/	
	private IPath createIPath(String p){
		return Path.EMPTY.append(p);
	}
	
	private void  check(String path, String oldName, String newName){
		IPath pOld= createIPath(path + "/" + oldName);
		String extension= "";
		//if (oldName.lastIndexOf(".") != -1)
		//	extension= oldName.substring(oldName.lastIndexOf("."));
		IPath pNew= createIPath(path + "/" + newName + extension);
		IPath newPath= RenameResourceChange.renamedResourcePath(pOld, newName);
		
		assertEquals(pNew.toString(), newPath.toString());
	}
	
/************/
	
	public void test0(){
		check("/s/p", "A.java", "B.java");
	}
	
	public void test1(){
		check("/s/p", "A.java", "A.java");
	}		

	public void test2(){
		check("/s/p", "A.txt", "B.txt");
	}		
	
	public void test3(){
		check("/s/p", "A", "B");
	}		
	
	public void test4(){
		check("/s/p/p", "A.java", "B.java");
	}
	
	public void test5(){
		check("/s/p/p", "A.java", "A.java");
	}		

	public void test6(){
		check("/s/p/p", "A.txt", "B.txt");
	}		
	
	public void test7(){
		check("/s", "A", "B.java");
	}		
	
	public void test8(){
		check("/s", "A.java", "B.java");
	}
	
	public void test9(){
		check("/s", "A.java", "A.java");
	}		

	public void test10(){
		check("/s", "A.txt", "B.txt");
	}		
	
	public void test11(){
		check("/s", "A", "B");
	}			
	
	public void test12(){
		check("", "A.java", "B.java");
	}
	
	public void test13(){
		check("", "A.java", "A.java");
	}		

	public void test14(){
		check("", "A.txt", "B.txt");
	}		
	
	public void test15(){
		check("", "A", "B");
	}			
}