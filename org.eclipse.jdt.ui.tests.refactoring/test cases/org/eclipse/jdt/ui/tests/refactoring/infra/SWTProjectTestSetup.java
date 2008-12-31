/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

	protected void setUp() throws Exception {
		super.setUp();
		fTestProject= new SWTTestProject();
	}

	protected void tearDown() throws Exception {
		fTestProject.delete();
		super.tearDown();
	}
}
