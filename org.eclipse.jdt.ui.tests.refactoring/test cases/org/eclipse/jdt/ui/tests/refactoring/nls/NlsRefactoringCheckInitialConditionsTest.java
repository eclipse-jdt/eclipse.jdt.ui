/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.refactoring.nls;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.corext.refactoring.nls.NLSRefactoring;

import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

public class NlsRefactoringCheckInitialConditionsTest {
	@Rule
	public ProjectTestSetup pts= new ProjectTestSetup();

	private NlsRefactoringTestHelper fHelper;
	private IJavaProject javaProject;

	@Before
	public void setUp() throws Exception {
		javaProject= pts.getProject();
		fHelper= new NlsRefactoringTestHelper(javaProject);
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(javaProject, pts.getDefaultClasspath());
	}

	protected String getRefactoringPath() {
		return "nls/"; //$NON-NLS-1$
	}

	@Test
	public void activationWithoutStrings() throws Exception {
		ICompilationUnit cu= fHelper.getCu("/TestSetupProject/src1/p/WithoutStrings.java"); //$NON-NLS-1$
		Refactoring refac= NLSRefactoring.create(cu);

		RefactoringStatus res= refac.checkInitialConditions(fHelper.fNpm);
		assertFalse("no nls needed", res.isOK()); //$NON-NLS-1$
	}

	@Test
	public void activationWithStrings() throws Exception {
		ICompilationUnit cu= fHelper.getCu("/TestSetupProject/src1/p/WithStrings.java"); //$NON-NLS-1$
		Refactoring refac= NLSRefactoring.create(cu);

		RefactoringStatus res= refac.checkInitialConditions(fHelper.fNpm);
		assertTrue("nls needed", res.isOK()); //$NON-NLS-1$
	}
}
