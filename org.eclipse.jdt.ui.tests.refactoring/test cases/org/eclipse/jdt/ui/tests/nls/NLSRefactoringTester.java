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
package org.eclipse.jdt.ui.tests.nls;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.internal.corext.refactoring.nls.NLSRefactoring;


public class NLSRefactoringTester extends TestCase {


	/**
	 * Constructor for NLSRefactoringTester
	 */
	public NLSRefactoringTester(String name) {
		super(name);
	}


	public static void main (String[] args) {
		junit.textui.TestRunner.run (suite());
	}
	
	public static Test suite() {
		return new TestSuite(NLSRefactoringTester.class);
	}
	
	private void testRemoveQuotes(String in, String expected){
		assertEquals("remove quotes", expected, NLSRefactoring.removeQuotes(in));
	}
	
	public void test0(){
		testRemoveQuotes("\"x\"", "x");
	}
	
	public void test1(){
		testRemoveQuotes("\"\"", "");	}

}


