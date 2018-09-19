/*******************************************************************************
 * Copyright (c) 2005, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.model;

import org.eclipse.jdt.junit.model.ITestRunSession;

public class TestRoot extends TestSuiteElement {

	private final ITestRunSession fSession;

	public TestRoot(ITestRunSession session) {
		super(null, "-1", session.getTestRunName(), 1, session.getTestRunName(), null, null); //$NON-NLS-1$
		fSession= session;
	}

	@Override
	public TestRoot getRoot() {
		return this;
	}

	@Override
	public ITestRunSession getTestRunSession() {
		return fSession;
	}
}
