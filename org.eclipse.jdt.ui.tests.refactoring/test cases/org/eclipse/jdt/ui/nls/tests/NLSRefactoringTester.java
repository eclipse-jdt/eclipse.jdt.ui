package org.eclipse.jdt.ui.nls.tests;

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

