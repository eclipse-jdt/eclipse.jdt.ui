/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public class PathTransformationTests extends TestCase {

	private static final Class clazz= PathTransformationTests.class;
	public PathTransformationTests(String name) {
		super(name);
	}

	public static Test suite() {
		return new RefactoringTestSetup(new TestSuite(clazz));
	}

	private IPath createIPath(String p){
		return Path.EMPTY.append(p);
	}

	private void  check(String path, String oldName, String newName){
		IPath pOld= createIPath(path + "/" + oldName);
		String extension= "";
		//if (oldName.lastIndexOf(".") != -1)
		//	extension= oldName.substring(oldName.lastIndexOf("."));
		IPath pNew= createIPath(path + "/" + newName + extension);
		IPath newPath= pOld.removeLastSegments(1).append(newName);

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
