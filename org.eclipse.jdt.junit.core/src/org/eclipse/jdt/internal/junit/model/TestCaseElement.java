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

import org.eclipse.core.runtime.Assert;

import org.eclipse.jdt.junit.model.ITestCaseElement;


public class TestCaseElement extends TestElement implements ITestCaseElement {

	private boolean fIgnored;

	public TestCaseElement(TestSuiteElement parent, String id, String testName) {
		super(parent, id, testName);
		Assert.isNotNull(parent);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.junit.ITestCaseElement#getTestMethodName()
	 */
	public String getTestMethodName() {
		int index= getTestName().indexOf('(');
		if (index > 0)
			return getTestName().substring(0, index);
		index= getTestName().indexOf('@');
		if(index > 0)
			return getTestName().substring(0, index);
		return getTestName();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.junit.ITestCaseElement#getTestCaseTypeName()
	 */
	public String getTestClassName() {
		return getClassName();
	}
	
	public void setIgnored(boolean ignored) {
		fIgnored= ignored;
	}
	
	public boolean isIgnored() {
		return fIgnored;
	}
	
	public String toString() {
		return "TestCase: " + getTestClassName() + "." + getTestMethodName() + " : " + super.toString(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}
