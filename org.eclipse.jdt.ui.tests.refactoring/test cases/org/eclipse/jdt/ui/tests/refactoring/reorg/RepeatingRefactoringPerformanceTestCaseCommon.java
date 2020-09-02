/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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

import org.eclipse.test.performance.Dimension;

import org.eclipse.jdt.ui.tests.refactoring.infra.RefactoringPerformanceTestCaseCommon;


public abstract class RepeatingRefactoringPerformanceTestCaseCommon extends RefactoringPerformanceTestCaseCommon {

	protected TestProject fTestProject;

	public TestProject getTestProject() {
		return fTestProject;
	}

	protected void executeRefactoring(int numberOfCus, int numberOfRefs, boolean measure, int sampleCount) throws Exception {
		for (int i= 0; i < sampleCount; i++) {
			try {
				fTestProject= new TestProject();
				doExecuteRefactoring(numberOfCus, numberOfRefs, measure);
			} finally {
				fTestProject.delete();
 			}
		}
		if (measure) {
			commitMeasurements();
			assertMeasurements();
		}
	}

	@Override
	protected void finishMeasurements() {
		stopMeasuring();
	}

	protected void assertMeasurements() {
		assertPerformanceInRelativeBand(Dimension.ELAPSED_PROCESS, -100, +10);
	}

	protected abstract void doExecuteRefactoring(int numberOfCus, int numberOfRefs, boolean measure) throws Exception;
}
