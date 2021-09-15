/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.quickfix;

import org.junit.After;
import org.junit.Rule;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.ui.tests.core.rules.Java16ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

public class QuickFixTestPreview extends QuickFixTest {

	@Rule
	public ProjectTestSetup projectsetup= new Java16ProjectTestSetup(true);

	private IJavaProject fJProject1;

//    private IPackageFragmentRoot fSourceFolder;

	@After
	public void tearDown() throws Exception {
		if (fJProject1 != null) {
			JavaProjectHelper.delete(fJProject1);
		}
	}

}
