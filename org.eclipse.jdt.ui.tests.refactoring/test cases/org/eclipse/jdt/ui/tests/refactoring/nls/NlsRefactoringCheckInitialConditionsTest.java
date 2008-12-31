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

	private NlsRefactoringTestHelper fHelper;
	private IJavaProject javaProject;

	public NlsRefactoringCheckInitialConditionsTest(String name) {
		super(name);
	}

	public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(NlsRefactoringCheckInitialConditionsTest.class));
	}

	public static Test suite() {
		return allTests();
	}

	protected void setUp() throws Exception {
		javaProject= ProjectTestSetup.getProject();
		fHelper= new NlsRefactoringTestHelper(javaProject);
	}

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
