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


public class TestCaseElement extends TestElement {

	private boolean fIgnored;

	public TestCaseElement(TestSuiteElement parent, String id, String testName) {
		super(parent, id, testName);
		Assert.isNotNull(parent);
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

	public void setIgnored(boolean ignored) {
		fIgnored= ignored;
	}
	
	public boolean isIgnored() {
		return fIgnored;
	}
}
