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

import org.eclipse.jdt.ui.tests.refactoring.rules.RefactoringPerformanceTestSetup;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MoveCompilationUnitPerfTests1 extends AbstractMoveCompilationUnitPrefTest {

	@Rule
	public RefactoringPerformanceTestSetup rpts= new RefactoringPerformanceTestSetup();

	@Test
	public void testACold_10_10() throws Exception {
		executeRefactoring(10, 10, false, 3);
	}

	@Test
	public void testB_10_10() throws Exception {
		executeRefactoring(10, 10, true, 3);
	}

	@Test
	public void testC_100_10() throws Exception {
		executeRefactoring(100, 10, true, 1);
	}

	@Test
	public void testD_1000_10() throws Exception {
		tagAsSummary("Move compilation units - 1000 CUs, 10 Refs", Dimension.ELAPSED_PROCESS);
		executeRefactoring(1000, 10, true, 1);
	}

	@Override
	protected void assertMeasurements() {
		assertPerformanceInRelativeBand(Dimension.CPU_TIME, -100, +10);
	}
}
