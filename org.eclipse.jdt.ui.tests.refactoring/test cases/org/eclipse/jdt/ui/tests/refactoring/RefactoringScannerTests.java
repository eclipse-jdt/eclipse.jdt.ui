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
package org.eclipse.jdt.ui.tests.refactoring;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringScanner;

public class RefactoringScannerTests extends RefactoringTest{

	public RefactoringScannerTests(String name){
		super(name);
	}
	
	private RefactoringScanner fScanner;
	private static Class clazz= RefactoringScannerTests.class;
	
	protected String getRefactoringPath() {
		return "RefactoringScanner/";
	}
	
	public static Test suite() {
		return new MySetup(new TestSuite(clazz));
	}
	
	protected void setUp() throws Exception {
		//no need to call super.setUp();
		fScanner= new RefactoringScanner();
		fScanner.setPattern("TestPattern");
	}

	protected void tearDown() throws Exception {
		//no need to call super.tearDown();
	}

	private void helper(String fileName, int expectedMatchCount, boolean analyzeComments, boolean analyzeJavaDoc, boolean analyzeString)	throws Exception{
		String text= getFileContents(getRefactoringPath() + fileName);
		fScanner.setAnalyzeComments(analyzeComments);
		fScanner.setAnalyzeJavaDoc(analyzeJavaDoc);
		fScanner.setAnalyzeStrings(analyzeString);
		fScanner.scan(text);
		assertEquals("results.length", expectedMatchCount, fScanner.getJavaDocResults().size() + fScanner.getCommentResults().size() + fScanner.getStringResults().size());		
	}
	
	//-- tests
	public void test0() throws Exception{
		String text= "";
		fScanner.scan(text);
		assertEquals("results.length", 0, fScanner.getJavaDocResults().size() + fScanner.getCommentResults().size() + fScanner.getStringResults().size());		
	}
	
	public void test1() throws Exception{
		helper("A.java", 8, true, true, true);
	}

	public void test2() throws Exception{
		helper("A.java", 4, false, true, true);
	}

	public void test3() throws Exception{
		helper("A.java", 6, true, false, true);
	}

	public void test4() throws Exception{
		helper("A.java", 6, true, true, false);
	}
	
	public void test5() throws Exception{
		helper("A.java", 2, false, false, true);
	}

	public void test6() throws Exception{
		helper("A.java", 2, false, true, false);
	}

	public void test7() throws Exception{
		helper("A.java", 4, true, false, false);
	}

	public void test8() throws Exception{
		helper("A.java", 0, false, false, false);
	}

	//---
	public void testWord1() throws Exception{
		helper("B.java", 6, true, true, true);
	}

	public void testWord2() throws Exception{
		helper("B.java", 3, false, true, true);
	}

	public void testWord3() throws Exception{
		helper("B.java", 5, true, false, true);
	}

	public void testWord4() throws Exception{
		helper("B.java", 4, true, true, false);
	}
	
	public void testWord5() throws Exception{
		helper("B.java", 2, false, false, true);
	}

	public void testWord6() throws Exception{
		helper("B.java", 1, false, true, false);
	}

	public void testWord7() throws Exception{
		helper("B.java", 3, true, false, false);
	}

	public void testWord8() throws Exception{
		helper("B.java", 0, false, false, false);
	}
		
}

