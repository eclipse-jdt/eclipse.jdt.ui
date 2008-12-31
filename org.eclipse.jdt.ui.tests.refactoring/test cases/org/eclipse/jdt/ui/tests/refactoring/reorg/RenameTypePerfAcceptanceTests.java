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

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.test.performance.Dimension;

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.refactoring.rename.RenameTypeProcessor;

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

	protected void finishMeasurements() {
		stopMeasuring();
		commitMeasurements();
		assertPerformanceInRelativeBand(Dimension.CPU_TIME, -100, +10);
	}

	public void testCold() throws Exception {
		IType control= fProject.findType("org.eclipse.swt.widgets.Control");
		RenameTypeProcessor processor= new RenameTypeProcessor(control);
		processor.setNewElementName("Control2");
		executeRefactoring(new RenameRefactoring(processor), false);
	}

	public void testWarm() throws Exception {
		tagAsSummary("Rename of Control", Dimension.ELAPSED_PROCESS);
		IType control= fProject.findType("org.eclipse.swt.widgets.Control2");
		RenameTypeProcessor processor= new RenameTypeProcessor(control);
		processor.setNewElementName("Control");
		executeRefactoring(new RenameRefactoring(processor), true);
	}
}
