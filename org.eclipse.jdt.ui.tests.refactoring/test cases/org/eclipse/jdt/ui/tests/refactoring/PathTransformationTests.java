/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.ui.tests.refactoring.rules.RefactoringTestSetup;

public class PathTransformationTests {

	@Rule
	public RefactoringTestSetup rts= new RefactoringTestSetup();

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

	@Test
	public void test0(){
		check("/s/p", "A.java", "B.java");
	}

	@Test
	public void test1(){
		check("/s/p", "A.java", "A.java");
	}

	@Test
	public void test2(){
		check("/s/p", "A.txt", "B.txt");
	}

	@Test
	public void test3(){
		check("/s/p", "A", "B");
	}

	@Test
	public void test4(){
		check("/s/p/p", "A.java", "B.java");
	}

	@Test
	public void test5(){
		check("/s/p/p", "A.java", "A.java");
	}

	@Test
	public void test6(){
		check("/s/p/p", "A.txt", "B.txt");
	}

	@Test
	public void test7(){
		check("/s", "A", "B.java");
	}

	@Test
	public void test8(){
		check("/s", "A.java", "B.java");
	}

	@Test
	public void test9(){
		check("/s", "A.java", "A.java");
	}

	@Test
	public void test10(){
		check("/s", "A.txt", "B.txt");
	}

	@Test
	public void test11(){
		check("/s", "A", "B");
	}

	@Test
	public void test12(){
		check("", "A.java", "B.java");
	}

	@Test
	public void test13(){
		check("", "A.java", "A.java");
	}

	@Test
	public void test14(){
		check("", "A.txt", "B.txt");
	}

	@Test
	public void test15(){
		check("", "A", "B");
	}
}
