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
package org.eclipse.jdt.ui.tests.refactoring.infra;

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.ui.tests.performance.JdtPerformanceTestCase;

import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.PerformRefactoringOperation;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class RefactoringPerformanceTestCase extends JdtPerformanceTestCase {
	
	public RefactoringPerformanceTestCase() {
		super();
	}
		
	public RefactoringPerformanceTestCase(String name) {
		super(name);
	}
	
	protected void executeRefactoring(Refactoring refactoring) throws Exception {
		executeRefactoring(refactoring, RefactoringStatus.WARNING);
	}
	
	protected void executeRefactoring(Refactoring refactoring, int maxSeverity) throws Exception {
		PerformRefactoringOperation operation= new PerformRefactoringOperation(refactoring, CheckConditionsOperation.ALL_CONDITIONS);
		joinBackgroudJobs();
		System.gc();
		startMeasuring();
		ResourcesPlugin.getWorkspace().run(operation, null);
		stopMeasuring();
		commitMeasurements();
		assertPerformance();
		assertEquals(true, operation.getConditionStatus().getSeverity() <= maxSeverity);
		assertEquals(true, operation.getValidationStatus().isOK());
		assertNotNull(operation.getUndoChange());
		RefactoringCore.getUndoManager().flush();
		System.gc();
	}
}
