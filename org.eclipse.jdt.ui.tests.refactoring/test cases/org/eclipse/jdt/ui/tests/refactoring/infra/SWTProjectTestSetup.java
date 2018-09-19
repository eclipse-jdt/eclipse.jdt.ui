/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.refactoring.infra;

import junit.framework.Test;

import org.eclipse.jdt.ui.tests.performance.SWTTestProject;

public class SWTProjectTestSetup extends RefactoringPerformanceTestSetup {

	private SWTTestProject fTestProject;

	public SWTProjectTestSetup(Test test) {
		super(test);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		fTestProject= new SWTTestProject();
	}

	@Override
	protected void tearDown() throws Exception {
		fTestProject.delete();
		super.tearDown();
	}
}
