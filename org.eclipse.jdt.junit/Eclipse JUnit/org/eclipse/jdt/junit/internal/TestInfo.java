/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.junit.internal;

/**
 * Util for storing information of tests that
 * were run.
 */
public class TestInfo {
	public String fTestName;
	public String fTrace;
	public int fStatus;

	public TestInfo(String testName){
		fTestName= testName;
	}	
}


