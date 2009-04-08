/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring.reorg;

import org.eclipse.test.performance.Dimension;

import org.eclipse.jdt.ui.tests.refactoring.infra.RefactoringPerformanceTestCase;


public abstract class RepeatingRefactoringPerformanceTestCase extends RefactoringPerformanceTestCase {

	protected TestProject fTestProject;

	public RepeatingRefactoringPerformanceTestCase(String name) {
		super(name);
	}

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

	protected void finishMeasurements() {
		stopMeasuring();
	}

	protected void assertMeasurements() {
		assertPerformanceInRelativeBand(Dimension.ELAPSED_PROCESS, -100, +10);
	}

	protected abstract void doExecuteRefactoring(int numberOfCus, int numberOfRefs, boolean measure) throws Exception;
}
