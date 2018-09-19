/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.corext.refactoring.nls.NLSRefactoring;

import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

public class NlsRefactoringCheckInitialConditionsTest extends TestCase {

	private static final Class<NlsRefactoringCheckInitialConditionsTest> THIS= NlsRefactoringCheckInitialConditionsTest.class;

	private NlsRefactoringTestHelper fHelper;
	private IJavaProject javaProject;

	public NlsRefactoringCheckInitialConditionsTest(String name) {
		super(name);
	}

	public static Test suite() {
		return setUpTest(new TestSuite(THIS));
	}

	public static Test setUpTest(Test test) {
		return new ProjectTestSetup(test);
	}

	@Override
	protected void setUp() throws Exception {
		javaProject= ProjectTestSetup.getProject();
		fHelper= new NlsRefactoringTestHelper(javaProject);
	}

	@Override
	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(javaProject, ProjectTestSetup.getDefaultClasspath());
	}

	protected String getRefactoringPath() {
		return "nls/"; //$NON-NLS-1$
	}

	public void testActivationWithoutStrings() throws Exception {
		ICompilationUnit cu= fHelper.getCu("/TestSetupProject/src1/p/WithoutStrings.java"); //$NON-NLS-1$
		Refactoring refac= NLSRefactoring.create(cu);

		RefactoringStatus res= refac.checkInitialConditions(fHelper.fNpm);
		assertFalse("no nls needed", res.isOK()); //$NON-NLS-1$
	}

	public void testActivationWithStrings() throws Exception {
		ICompilationUnit cu= fHelper.getCu("/TestSetupProject/src1/p/WithStrings.java"); //$NON-NLS-1$
		Refactoring refac= NLSRefactoring.create(cu);

		RefactoringStatus res= refac.checkInitialConditions(fHelper.fNpm);
		assertTrue("nls needed", res.isOK()); //$NON-NLS-1$
	}
}
