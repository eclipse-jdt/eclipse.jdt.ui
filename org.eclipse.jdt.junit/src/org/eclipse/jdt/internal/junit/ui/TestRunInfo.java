/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.ui;

/**
 * Store information about an executed test.
 */
public class TestRunInfo extends Object {
	private String fTestId;
	private String fTestName;
	private String fTrace;
	private String fExpected;
	private String fActual;
	
	private int fStatus;

	public TestRunInfo(String testId, String testName){
		fTestName= testName;
		fTestId= testId;
	}	
	
	/*
	 * @see Object#hashCode()
	 */
	public int hashCode() {
		return getTestId().hashCode();
	}

	/*
	 * @see Object#equals(Object)
	 */
	public boolean equals(Object obj) {
		return getTestId().equals(obj);
	}

	public String getTestId() {
		return fTestId;
	}

	public String getTestName() {
		return fTestName;
	}

	public String getClassName() {
		return extractClassName(getTestName());
	}
	
	public String getTestMethodName() {
		int index= fTestName.indexOf('(');
		if (index > 0)
			return fTestName.substring(0, index);
		index= fTestName.indexOf('@');
		if(index > 0)
			return fTestName.substring(0, index);
		return fTestName;
	}
	
	private String extractClassName(String testNameString) {
		if (testNameString == null) 
			return null;
		int index= testNameString.indexOf('(');
		if (index < 0) 
			return testNameString;
		testNameString= testNameString.substring(index + 1);
		return testNameString.substring(0, testNameString.indexOf(')'));
	}		

	public void setTrace(String trace) {
		fTrace= trace;
	}

	public String getTrace() {
		return fTrace;
	}

	public void setStatus(int status) {
		fStatus= status;
	}

	public int getStatus() {
		return fStatus;
	}
	
    public String getActual() {
        return fActual;
    }
    
    public void setActual(String actual) {
        fActual = actual;
    }
    
    public String getExpected() {
        return fExpected;
    }
    
    public void setExpected(String expected) {
        fExpected = expected;
    }
    
    public boolean isComparisonFailure() {
        return fExpected != null && fActual != null;
    }
}
