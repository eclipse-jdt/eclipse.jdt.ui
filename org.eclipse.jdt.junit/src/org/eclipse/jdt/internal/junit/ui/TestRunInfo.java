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
package org.eclipse.jdt.internal.junit.ui;

/**
 * Store information about an executed test.
 */
public class TestRunInfo extends Object {
	private String fTestId;
	private String fTestName;
	private String fTrace;
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
}




