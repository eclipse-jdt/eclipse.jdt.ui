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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;

import org.eclipse.jdt.ui.tests.performance.JdtPerformanceTestCase;

public abstract class RefactoringPerformanceTestCase extends JdtPerformanceTestCase {
	
	private RefactoringStatus fStatus;
	private IChange fUndo;
	
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
	
	protected void executeRefactoring(final Refactoring refactoring, boolean measure, int maxSeverity, boolean checkUndo) throws Exception {
		joinBackgroudActivities();
		System.gc();
		if (measure)
			startMeasuring();
		ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				performRefactoring(refactoring);
			}
		}, null);
		if (measure)
			finishMeasurements();
		assertEquals(true, fStatus.getSeverity() <= maxSeverity);
		if (checkUndo) {
			assertNotNull(fUndo);
		}
		Refactoring.getUndoManager().flush();
		System.gc();
	}
	
	private void performRefactoring(IRefactoring ref) throws JavaModelException {
		fStatus= ref.checkPreconditions(new NullProgressMonitor());
		if (fStatus.hasFatalError())
			return;

		IChange change= ref.createChange(new NullProgressMonitor());
		performChange(change);

		fUndo= change.getUndoChange();
		Refactoring.getUndoManager().addUndo(ref.getName(), fUndo);

		return;
	}
	
	private void performChange(IChange change) throws JavaModelException{
		change.aboutToPerform(new ChangeContext(new TestExceptionHandler()), new NullProgressMonitor());
		try {
			change.perform(new ChangeContext(new TestExceptionHandler()), new NullProgressMonitor());
		} finally {
			change.performed();
		}
	}
}
