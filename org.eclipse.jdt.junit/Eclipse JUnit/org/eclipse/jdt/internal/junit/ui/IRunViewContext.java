/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.junit.ui;

/**
 * The interface RunView gets for accessing the TestRunner
 * functionality.
 */
public interface IRunViewContext {
	public TestInfo getTestInfo(String testName);
	public void handleTestSelected(String testName);
	public void goToTest(String testName, int lineNumber);
	public void goToTestMethod(String testName, String methodName);
	public void reRunTest(String[] classNames);	
}


