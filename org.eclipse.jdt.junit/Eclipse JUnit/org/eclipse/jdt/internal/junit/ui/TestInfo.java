/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.junit.ui;


/**
 * Info object storing test about an executed test.
 */
public class TestInfo {
	public String fTestName;
	public String fTrace;
	public int fStatus;

	public TestInfo(String testName){
		fTestName= testName;
	}	
}




