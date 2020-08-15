/*******************************************************************************
 * Copyright (c) 2005, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.refactoring.infra;

import org.junit.Assert;

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.PerformRefactoringOperation;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringCore;

public abstract class RefactoringHeapTestCase extends RefactoringPerformanceTestCaseCommon {

	@Override
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
		if (checkUndo)
			Assert.assertNotNull(operation.getUndoChange());
		Assert.assertTrue(operation.getConditionStatus().getSeverity() <= maxSeverity);
		Assert.assertTrue(operation.getValidationStatus().isOK());
		RefactoringCore.getUndoManager().flush();
		System.gc();
		if (measure)
			finishMeasurements();
	}
}