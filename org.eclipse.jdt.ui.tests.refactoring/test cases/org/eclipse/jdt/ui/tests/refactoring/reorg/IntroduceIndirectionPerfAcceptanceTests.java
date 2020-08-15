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
package org.eclipse.jdt.ui.tests.refactoring.reorg;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.refactoring.code.IntroduceIndirectionRefactoring;

import org.eclipse.jdt.ui.tests.performance.SWTTestProject;
import org.eclipse.jdt.ui.tests.refactoring.infra.RefactoringPerformanceTestCaseCommon;
import org.eclipse.jdt.ui.tests.refactoring.rules.SWTProjectTestSetup;

public class IntroduceIndirectionPerfAcceptanceTests extends RefactoringPerformanceTestCaseCommon {

	private IJavaProject fProject;
	private IntroduceIndirectionRefactoring fRefactoring;

	@Rule
	public SWTProjectTestSetup spts= new SWTProjectTestSetup();

	@Override
	public void setUp() throws Exception {
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

	@Test
	public void testIntroduceIndirection() throws Exception {
		executeRefactoring(fRefactoring, true);
	}
}
