/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.junit.internal;

public interface IRunViewContext {

	public TestInfo getTestInfo(String testName);
	public void handleTestSelected(String testName);
	public void goToFile(String testName, int lineNumber);
	public void goToFile(String testName, String methodName);
	public void reRunTest(String[] classNames);	
}

