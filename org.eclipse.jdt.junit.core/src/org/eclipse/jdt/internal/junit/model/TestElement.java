/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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

import org.eclipse.jdt.junit.ITestRunListener;


public abstract class TestElement {
	public final static class Status {
		public static final Status RUNNING_ERROR= new Status("RUNNING_ERROR", -2,   3); //$NON-NLS-1$
		public static final Status RUNNING_FAILURE= new Status("RUNNING_FAILURE", -1,   3); //$NON-NLS-1$
		public static final Status RUNNING= new Status("RUNNING", 0,   3); //$NON-NLS-1$
		public static final Status ERROR=   new Status("ERROR",   1, /*1*/ITestRunListener.STATUS_ERROR); //$NON-NLS-1$
		public static final Status FAILURE= new Status("FAILURE", 2, /*2*/ITestRunListener.STATUS_FAILURE); //$NON-NLS-1$
		public static final Status OK=      new Status("OK",      3, /*0*/ITestRunListener.STATUS_OK); //$NON-NLS-1$
		public static final Status NOT_RUN= new Status("NOT_RUN", 4,   4); //$NON-NLS-1$
		
		private static final Status[] OLD_CODE= { OK, ERROR, FAILURE};
		
		private final String fName;
		private final int fPriority;
		private final int fOldCode;
		
		private Status(String name, int priority, int oldCode) {
			fName= name;
			fPriority= priority;
			fOldCode= oldCode;
		}
		
		/**
		 * @param a a status
		 * @param b another status
		 * @return the status with higher precedence
		 */
		public static Status getCombinedStatus(Status a, Status b) {
			if (a.fPriority < b.fPriority)
				return a;
			else
				return b;
		}
		
		public int getOldCode() {
			return fOldCode;
		}
		
		public String toString() {
			return fName;
		}

		/**
		 * @param oldStatus one of {@link ITestRunListener}'s STATUS_* constants
		 * @return the Status
		 */
		public static Status convert(int oldStatus) {
			return OLD_CODE[oldStatus];
		}

		/**
		 * @return <code>true</code> iff this is a {@link #FAILURE} or an {@link #ERROR}
		 */
		public boolean isFailure() {
			return this == FAILURE || this == ERROR;
		}
	}
	
	private final TestSuiteElement fParent;
	private final String fId;
	private final String fTestName;

	/**
	 * @param parent the parent, can be <code>null</code>
	 * @param id the test id
	 * @param testName the test name
	 */
	public TestElement(TestSuiteElement parent, String id, String testName) {
		Assert.isNotNull(id);
		Assert.isNotNull(testName);
		fParent= parent;
		fId= id;
		fTestName= testName;
		if (parent != null)
			parent.addChild(this);
	}
	
	/**
	 * @return the parent suite, or <code>null</code> for the root
	 */
	public TestSuiteElement getParent() {
		return fParent;
	}
	
	public String getId() {
		return fId;
	}
	
	public String getTestName() {
		return fTestName;
	}
	
	// TODO: Format of testName is highly underspecified. See RemoteTestRunner#getTestName(Test).
	
	public String getClassName() {
		return extractClassName(getTestName());
	}
	
	private String extractClassName(String testNameString) {
		int index= testNameString.indexOf('(');
		if (index < 0) 
			return testNameString;
		testNameString= testNameString.substring(index + 1);
		return testNameString.substring(0, testNameString.indexOf(')'));
	}
	
	public abstract Status getStatus();
	
	public TestRoot getRoot() {
		return getParent().getRoot();
	}
	
	public String toString() {
		return getTestName() + ": " + getStatus(); //$NON-NLS-1$
	}
}
