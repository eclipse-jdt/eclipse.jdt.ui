/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.junit.ui;

/**
 * Store information about an executed test.
 */
public class TestRunInfo extends Object {
	public String fTestName;
	public String fTrace;
	public int fStatus;

	public TestRunInfo(String testName){
		fTestName= testName;
	}	
	
	/*
	 * @see Object#hashCode()
	 */
	public int hashCode() {
		return fTestName.hashCode();
	}

	/*
	 * @see Object#equals(Object)
	 */
	public boolean equals(Object obj) {
		return fTestName.equals(obj);
	}
}




