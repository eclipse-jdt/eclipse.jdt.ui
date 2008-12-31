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
package org.eclipse.jdt.ui.tests.refactoring.infra;

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.PerformRefactoringOperation;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.ui.tests.performance.JdtPerformanceTestCase;

public abstract class RefactoringPerformanceTestCase extends JdtPerformanceTestCase {

	public RefactoringPerformanceTestCase() {
		super();
	}

	public RefactoringPerformanceTestCase(String name) {
		super(name);
	}

	protected void executeRefactoring(Refactoring refactoring, boolean measure) throws Exception {
		executeRefactoring(refactoring, measure, RefactoringStatus.WARNING);
	}

	protected void executeRefactoring(Refactoring refactoring, boolean measure, int maxSeverity) throws Exception {
		executeRefactoring(refactoring, measure, maxSeverity, true);
	}

	protected void executeRefactoring(Refactoring refactoring, boolean measure, int maxSeverity, boolean checkUndo) throws Exception {
		PerformRefactoringOperation operation= new PerformRefactoringOperation(refactoring, CheckConditionsOperation.ALL_CONDITIONS);
		joinBackgroudActivities();
		// Flush the undo manager to not count any already existing undo objects
		// into the heap consumption
		RefactoringCore.getUndoManager().flush();
		System.gc();
		if (measure)
			startMeasuring();
		ResourcesPlugin.getWorkspace().run(operation, null);
		if (measure)
			finishMeasurements();
		assertEquals(true, operation.getConditionStatus().getSeverity() <= maxSeverity);
		assertEquals(true, operation.getValidationStatus().isOK());
		if (checkUndo) {
			assertNotNull(operation.getUndoChange());
		}
		RefactoringCore.getUndoManager().flush();
		System.gc();
	}
}
