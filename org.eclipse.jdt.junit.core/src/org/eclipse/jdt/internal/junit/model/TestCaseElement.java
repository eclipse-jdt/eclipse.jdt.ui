/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.junit.model;

import org.eclipse.jface.util.Assert;


public class TestCaseElement extends TestElement {

	private Status fStatus;
	private String fTrace;
	private String fExpected;
	private String fActual;
	
	public TestCaseElement(TestSuiteElement parent, String id, String testName) {
		super(parent, id, testName);
		Assert.isNotNull(parent);
		fStatus= Status.NOT_RUN;
	}
	
	public void setStatus(Status status) {
		//TODO: notify about change?
		//TODO: multiple errors/failures per test https://bugs.eclipse.org/bugs/show_bug.cgi?id=125296
		fStatus= status;
	}
	
	public void setStatus(Status status, String trace, String expected, String actual) {
		//TODO: notify about change?
		//TODO: multiple errors/failures per test https://bugs.eclipse.org/bugs/show_bug.cgi?id=125296
		fStatus= status;
		fTrace= trace;
		fExpected= expected;
		fActual= actual;
	}

	public Status getStatus() {
		return fStatus;
	}
	
	public String getTrace() {
		return fTrace;
	}		
	
	public String getExpected() {
		return fExpected;
	}		
	
	public String getActual() {
		return fActual;
	}		
	
	public boolean isComparisonFailure() {
		return fExpected != null && fActual != null;
	}
	
	// TODO: Format of testName is highly underspecified. See RemoteTestRunner#getTestName(Test).

	public String getClassName() {
		return extractClassName(getTestName());
	}
	
	public String getTestMethodName() {
		int index= getTestName().indexOf('(');
		if (index > 0)
			return getTestName().substring(0, index);
		index= getTestName().indexOf('@');
		if(index > 0)
			return getTestName().substring(0, index);
		return getTestName();
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
}
