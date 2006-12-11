/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.model;

import org.eclipse.jdt.junit.model.ITestRunSession;

public class TestRoot extends TestSuiteElement {

	private final ITestRunSession fSession;

	public TestRoot(ITestRunSession session) {
		super(null, "-1", "TESTROOT", 1); //$NON-NLS-1$//$NON-NLS-2$
		fSession= session;
	}

	public TestRoot getRoot() {
		return this;
	}
	
	public ITestRunSession getTestRunSession() {
		return fSession;
	}
}
