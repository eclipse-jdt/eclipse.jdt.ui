/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.text.Assert;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.ui.actions.WorkspaceModifyOperation;

import org.eclipse.jdt.internal.corext.refactoring.base.ChangeAbortException;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.IUndoManager;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;

import org.eclipse.jdt.internal.ui.refactoring.changes.AbortChangeExceptionHandler;
import org.eclipse.jdt.internal.ui.refactoring.changes.ChangeExceptionHandler;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class RefactoringOperation {

	private int fLifeCycle;
	private RefactoringStatus fStatus;
	private IChange fChange;
	private Refactoring fRefactoring;

	public RefactoringOperation(Refactoring refactoring) {
		super();
		Assert.isNotNull(refactoring);
		fRefactoring= refactoring;
	}
	
	public void reset() {
		fStatus= null;
		fChange= null;
	}

	public RefactoringStatus checkPreconditions(IRunnableContext context) throws InvocationTargetException, InterruptedException {
		CheckConditionsOperation op= new CheckConditionsOperation(
			fRefactoring, CheckConditionsOperation.PRECONDITIONS);
		context.run(true, true, op);
		fStatus= op.getStatus();
		return fStatus;
	}

	public RefactoringStatus getStatus() {
		return fStatus;
	}
	
	public boolean createChange(IRunnableContext context, int stopSeverity) throws InvocationTargetException, InterruptedException {
		CreateChangeOperation op= createChangeOperation(stopSeverity);
		context.run(true, true, op);
		if (fStatus == null)
			fStatus= op.getStatus();
		fChange= op.getChange();
		return fChange != null;	
	}

	public IChange getChange() {
		return fChange;
	}
	
	public boolean performChange(Shell parent, IRunnableContext execContext, int stopSeverity) throws InvocationTargetException, InterruptedException {
		if (fStatus != null && fStatus.hasFatalError())
			return false;
		PerformChangeOperation op= null;
		if (fChange != null) {
			op= new PerformChangeOperation(fChange);
		} else {
			op= new PerformChangeOperation(createChangeOperation(stopSeverity));
		}
		op.setCheckPassedSeverity(stopSeverity - 1);
		ChangeContext context= new ChangeContext(new ChangeExceptionHandler(parent));
		boolean success= false;
		IUndoManager undoManager= Refactoring.getUndoManager();
		try{
			op.setChangeContext(context);
			undoManager.aboutToPerformRefactoring();
			execContext.run(false, false, op);
			if (op.changeExecuted()) {
				if (! op.getChange().isUndoable()){
					success= false;
				} else { 
					undoManager.addUndo(fRefactoring.getName(), op.getChange().getUndoChange());
					success= true;
				}	
			}
		} catch (InvocationTargetException e) {
			Throwable t= e.getTargetException();
			if (t instanceof ChangeAbortException) {
				success= handleChangeAbortException(execContext, context, (ChangeAbortException)t);
				return false;
			}
			throw e;
		} finally {
			context.clearPerformedChanges();
			undoManager.refactoringPerformed(success);
		}
		if (fStatus == null)
			fStatus= op.getStatus();
		if (fChange != null)
			fChange= op.getChange();
		return fChange != null;
	}
	
	private boolean handleChangeAbortException(IRunnableContext execContext, final ChangeContext context, ChangeAbortException exception) {
		if (!context.getTryToUndo())
			return false; // Return false since we handle an unexpected exception and we don't have any
						  // idea in which state the workbench is.
			
		WorkspaceModifyOperation op= new WorkspaceModifyOperation() {
			protected void execute(IProgressMonitor pm) throws CoreException, InvocationTargetException {
				ChangeContext undoContext= new ChangeContext(new AbortChangeExceptionHandler());
				try {
					IChange[] changes= context.getPerformedChanges();
					pm.beginTask(RefactoringMessages.getString("RefactoringWizard.undoing"), changes.length); //$NON-NLS-1$
					IProgressMonitor sub= new NullProgressMonitor();
					for (int i= changes.length - 1; i >= 0; i--) {
						IChange change= changes[i];
						pm.subTask(change.getName());
						change.getUndoChange().perform(undoContext, sub);
						pm.worked(1);
					}
				} catch (ChangeAbortException e) {
					throw new InvocationTargetException(e.getThrowable());
				} finally {
					pm.done();
				} 
			}
		};
		
		try {
			execContext.run(false, false, op);
		} catch (InvocationTargetException e) {
			handleUnexpectedException(e);
			return false;
		} catch (InterruptedException e) {
			// not possible. Operation not cancelable.
		}
		
		return true;
	}
	
	private void handleUnexpectedException(InvocationTargetException e) {
		ExceptionHandler.handle(e, RefactoringMessages.getString("RefactoringWizard.refactoring"), RefactoringMessages.getString("RefactoringWizard.unexpected_exception_1")); //$NON-NLS-2$ //$NON-NLS-1$
	}

	private CreateChangeOperation createChangeOperation(int stopSeverity) {
		if (fStatus == null) {
			return new CreateChangeOperation(fRefactoring, CreateChangeOperation.CHECK_PRECONDITION, stopSeverity - 1); 
		} else {
			return new CreateChangeOperation(fRefactoring, CreateChangeOperation.CHECK_NONE, stopSeverity - 1);
		}
	}
}
