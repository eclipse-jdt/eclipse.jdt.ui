/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring.reorg;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.test.performance.Dimension;

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.refactoring.rename.RenameTypeRefactoring;

import org.eclipse.jdt.ui.tests.performance.SWTTestProject;
import org.eclipse.jdt.ui.tests.refactoring.infra.RefactoringPerformanceTestCase;
import org.eclipse.jdt.ui.tests.refactoring.infra.SWTProjectTestSetup;

public class RenameTypePerfAcceptanceTests extends RefactoringPerformanceTestCase {
	
	private IJavaProject fProject;
	
	public static Test suite() {
		// we must make sure that cold is executed before warm
		TestSuite suite= new TestSuite("RenameTypePerfAcceptanceTests");
		suite.addTest(new RenameTypePerfAcceptanceTests("testCold"));
		suite.addTest(new RenameTypePerfAcceptanceTests("testWarm"));
        return new SWTProjectTestSetup(suite);
	}

	public static Test setUpTest(Test someTest) {
		return new SWTProjectTestSetup(someTest);
	}

	public RenameTypePerfAcceptanceTests(String test) {
		super(test);
	}
	
	protected void setUp() throws Exception {
		super.setUp();
		fProject= (IJavaProject)JavaCore.create(
			ResourcesPlugin.getWorkspace().getRoot().findMember(SWTTestProject.PROJECT));
	}
	
	public void testCold() throws Exception {
		IType control= fProject.findType("org.eclipse.swt.widgets.Control");
		RenameTypeRefactoring refactoring= new RenameTypeRefactoring(control);
		refactoring.setNewName("Control2");
		executeRefactoring(refactoring, false);
	}
	
	public void testWarm() throws Exception {
		tagAsGlobalSummary("Rename of Control", new Dimension[] {Dimension.CPU_TIME, Dimension.USED_JAVA_HEAP});
		IType control= fProject.findType("org.eclipse.swt.widgets.Control2");
		RenameTypeRefactoring refactoring= new RenameTypeRefactoring(control);
		refactoring.setNewName("Control");
		executeRefactoring(refactoring, true);
	}
}
