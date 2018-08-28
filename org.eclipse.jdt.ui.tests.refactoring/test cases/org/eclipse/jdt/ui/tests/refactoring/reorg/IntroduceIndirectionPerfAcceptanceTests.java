/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.refactoring.reorg;

import junit.framework.Test;

import org.junit.Assert;

import org.eclipse.test.OrderedTestSuite;

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.refactoring.code.IntroduceIndirectionRefactoring;

import org.eclipse.jdt.ui.tests.performance.SWTTestProject;
import org.eclipse.jdt.ui.tests.refactoring.infra.RefactoringPerformanceTestCase;
import org.eclipse.jdt.ui.tests.refactoring.infra.SWTProjectTestSetup;

public class IntroduceIndirectionPerfAcceptanceTests extends RefactoringPerformanceTestCase {

	private IJavaProject fProject;
	private IntroduceIndirectionRefactoring fRefactoring;

	public static Test suite() {
		OrderedTestSuite suite= new OrderedTestSuite(IntroduceIndirectionPerfAcceptanceTests.class, new String[] {
			"testIntroduceIndirection",
		});
        return new SWTProjectTestSetup(suite);
	}

	public static Test setUpTest(Test someTest) {
		return new SWTProjectTestSetup(someTest);
	}

	public IntroduceIndirectionPerfAcceptanceTests(String test) {
		super(test);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		fProject= (IJavaProject)JavaCore.create(
			ResourcesPlugin.getWorkspace().getRoot().findMember(SWTTestProject.PROJECT));

		IType control= fProject.findType("org.eclipse.swt.widgets.Widget");
		IMethod m= control.getMethod("getDisplay", new String[0]);
		Assert.assertTrue(m != null && m.exists());
		fRefactoring= new IntroduceIndirectionRefactoring(m);
		fRefactoring.setEnableUpdateReferences(true);
		fRefactoring.setIntermediaryTypeName("org.eclipse.swt.widgets.Widget");
		fRefactoring.setIntermediaryMethodName("bar");
	}

	public void testIntroduceIndirection() throws Exception {
		executeRefactoring(fRefactoring, true);
	}
}
