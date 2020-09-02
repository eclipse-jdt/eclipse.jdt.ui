/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.refactoring.rules;

import org.eclipse.jdt.ui.tests.performance.SWTTestProject;

public class SWTProjectTestSetup extends RefactoringPerformanceTestSetup {

	private SWTTestProject fTestProject;

	@Override
	public void before() throws Exception {
		super.before();
		fTestProject= new SWTTestProject();
	}

	@Override
	public void after() {
		try {
			fTestProject.delete();
		} catch (Exception e) {
			e.printStackTrace();
		}
		super.after();
	}
}
