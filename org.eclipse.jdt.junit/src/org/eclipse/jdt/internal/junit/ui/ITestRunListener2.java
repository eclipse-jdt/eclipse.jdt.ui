/*
 * Created on Feb 8, 2003
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
package org.eclipse.jdt.internal.junit.ui;

import org.eclipse.jdt.junit.ITestRunListener;

/**
 * Extends ITestRunListener with a call back to trace the test contents
 */
public interface ITestRunListener2 extends ITestRunListener {

	/**
	 * Information about a member of the test suite that is about to be run.
	 * The format of the string is: 
	 * <pre>
	 * testName","isSuite","testcount
	 * 
	 * testName: the name of the test
	 * isSuite: true or false depending on whether the test is a suite
	 * testCount: an integer indicating the number of tests 
	 * 
	 * Example: "testPass(junit.tests.MyTest),false,1"
	 * </pre>
	 * 
	 * @param entry
	 */ 
	public void testTreeEntry(String description);
}
