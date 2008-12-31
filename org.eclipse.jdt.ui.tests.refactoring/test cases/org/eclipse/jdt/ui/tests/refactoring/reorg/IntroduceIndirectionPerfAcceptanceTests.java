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
package org.eclipse.jdt.ui.tests.refactoring.reorg;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestSuite;

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
		TestSuite suite= new TestSuite("IntroduceIndirectionPerfAcceptanceTests");
		suite.addTest(new IntroduceIndirectionPerfAcceptanceTests("testIntroduceIndirection"));
        return new SWTProjectTestSetup(suite);
	}

	public static Test setUpTest(Test someTest) {
		return new SWTProjectTestSetup(someTest);
	}

	public IntroduceIndirectionPerfAcceptanceTests(String test) {
		super(test);
	}

	protected void setUp() throws Exception {
		super.setUp();
		fProject= (IJavaProject)JavaCore.create(
			ResourcesPlugin.getWorkspace().getRoot().findMember(SWTTestProject.PROJECT));

		IType control= fProject.findType("org.eclipse.swt.widgets.Widget");
		IMethod m= control.getMethod("getDisplay", new String[0]);
		Assert.assertTrue(m != null && m.exists());
		fRefactoring= new IntroduceIndirectionRefactoring(m);
		fRefactoring.setEnableUpdateReferences(true);
		fRefactoring.setIntermediaryClassName("org.eclipse.swt.widgets.Widget");
		fRefactoring.setIntermediaryMethodName("bar");
	}

	public void testIntroduceIndirection() throws Exception {
		executeRefactoring(fRefactoring, true);
	}
}
