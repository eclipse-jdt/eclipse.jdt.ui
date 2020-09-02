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

import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import org.eclipse.test.performance.Dimension;

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.refactoring.rename.RenameTypeProcessor;

import org.eclipse.jdt.ui.tests.performance.SWTTestProject;
import org.eclipse.jdt.ui.tests.refactoring.infra.RefactoringPerformanceTestCaseCommon;
import org.eclipse.jdt.ui.tests.refactoring.rules.SWTProjectTestSetup;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RenameTypePerfAcceptanceTests extends RefactoringPerformanceTestCaseCommon {

	private IJavaProject fProject;

	@Rule
	public SWTProjectTestSetup spts= new SWTProjectTestSetup();

	@Override
	public void setUp() throws Exception {
		super.setUp();
		fProject= (IJavaProject)JavaCore.create(
			ResourcesPlugin.getWorkspace().getRoot().findMember(SWTTestProject.PROJECT));
	}

	@Override
	protected void finishMeasurements() {
		stopMeasuring();
		commitMeasurements();
		assertPerformanceInRelativeBand(Dimension.CPU_TIME, -100, +10);
	}

	@Test
	public void testCold() throws Exception {
		IType control= fProject.findType("org.eclipse.swt.widgets.Control");
		RenameTypeProcessor processor= new RenameTypeProcessor(control);
		processor.setNewElementName("Control2");
		executeRefactoring(new RenameRefactoring(processor), false);
	}

	@Test
	public void testWarm() throws Exception {
		tagAsSummary("Rename of Control", Dimension.ELAPSED_PROCESS);
		IType control= fProject.findType("org.eclipse.swt.widgets.Control2");
		RenameTypeProcessor processor= new RenameTypeProcessor(control);
		processor.setNewElementName("Control");
		executeRefactoring(new RenameRefactoring(processor), true);
	}
}
