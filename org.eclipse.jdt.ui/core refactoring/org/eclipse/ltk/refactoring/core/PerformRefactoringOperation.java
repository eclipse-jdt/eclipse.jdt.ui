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
package org.eclipse.ltk.refactoring.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IWorkspaceRunnable;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.base.Change;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;

import org.eclipse.jdt.internal.ui.refactoring.CheckConditionsOperation;
import org.eclipse.jdt.internal.ui.refactoring.CreateChangeOperation;
import org.eclipse.jdt.internal.ui.refactoring.PerformChangeOperation;


public class PerformRefactoringOperation implements IWorkspaceRunnable {
	
	private int fStyle;
	private Refactoring fRefactoring;
	
	private RefactoringStatus fPreconditionStatus;
	private RefactoringStatus fValidationStatus;
	private Change fUndo;
	
	public static final int CHECK_NONE=         CheckConditionsOperation.NONE;
	public static final int CHECK_ACTIVATION=   CheckConditionsOperation.ACTIVATION;
	public static final int CHECK_INPUT=        CheckConditionsOperation.INPUT;
	public static final int CHECK_PRECONDITION= CheckConditionsOperation.PRECONDITIONS;
	
	public PerformRefactoringOperation(Refactoring refactoring, int style) {
		Assert.isNotNull(refactoring);
		fRefactoring= refactoring;
		fStyle= style;
	}
	
	public RefactoringStatus getConditionStatus() {
		return fPreconditionStatus;
	}
	
	public RefactoringStatus getValidationStatus() {
		return fValidationStatus;
	}
	
	public Change getUndoChange() {
		return fUndo;
	}

	public void run(IProgressMonitor monitor) throws CoreException {
		monitor.beginTask("", 10); //$NON-NLS-1$
		CreateChangeOperation create= new CreateChangeOperation(fRefactoring, fStyle);
		create.run(new SubProgressMonitor(monitor, 6));
		fPreconditionStatus= create.getStatus();
		if (fPreconditionStatus.hasFatalError()) {
			monitor.done();
			return;
		}
		Change change= create.getChange();
		PerformChangeOperation perform= new PerformChangeOperation(change);
		perform.setUndoManager(Refactoring.getUndoManager(), fRefactoring.getName());
		perform.run(new SubProgressMonitor(monitor, 2));
		fValidationStatus= perform.getValidationStatus();
		fUndo= perform.getUndoChange();
		fUndo.initializeValidationData(new SubProgressMonitor(monitor, 1));
	}
}
