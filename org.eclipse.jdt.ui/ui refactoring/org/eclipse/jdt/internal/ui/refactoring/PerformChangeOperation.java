/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IWorkspaceRunnable;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.util.Assert;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.internal.corext.refactoring.base.ChangeAbortException;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.IUndoManager;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;

import org.eclipse.jdt.internal.ui.IJavaStatusConstants;
import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Operation that, when performed, performes a change to the workbench.
 */
public class PerformChangeOperation implements IRunnableWithProgress {

	private IChange fChange;
	private ChangeContext fChangeContext;
	private CreateChangeOperation fCreateChangeOperation;
	private int fCheckPassedSeverity;
	private boolean fChangeExecuted;
	private RefactoringStatus fValidationStatus;
	
	private String fUndoName;
	private IUndoManager fUndoManager;
	
	/**
	 * Creates a new perform change operation instance for the given change.
	 * 
	 * @param change the change to be applied to the workbench
	 */
	public PerformChangeOperation(IChange change) {
		fChange= change;
		Assert.isNotNull(fChange);
	}

	/**
	 * Creates a new perform change operation for the given create change operation. 
	 * When executed, a new change is created using the passed instance.
	 * 
	 * @param op the <code>CreateChangeOperation</code> used to create a new
	 *  change object
	 */
	public PerformChangeOperation(CreateChangeOperation op) {
		fCreateChangeOperation= op;
		Assert.isNotNull(fCreateChangeOperation);
		fCheckPassedSeverity= RefactoringStatus.INFO;
	}

	/**
	 * Returns <code>true</code> if the change has been executed. Otherwise <code>false</code>
	 * is returned.
	 * 
	 * @return <code>true</code> if the change has been executed, otherwise
	 *  <code>false</code>
	 */
	public boolean changeExecuted() {
		return fChangeExecuted;
	}
	
	/**
	 * Returns the change used by this operation. This is either the change passed to
	 * the constructor or the one create by the <code>CreateChangeOperation</code>.
	 * Method returns <code>null</code> if the create operation did not create
	 * a change.
	 * 
	 * @return the change used by this operation or <code>null</code> if no change
	 *  has been created
	 */
	public IChange getChange() {
		return fChange;
	}
	
	/**
	 * Returns the refactoring status returned from call <code>IChange#isValid()</code>.
	 * Returns <code>null</code> if the change wasn't executed.
	 * 
	 * @return the change's validation status
	 */
	public RefactoringStatus getValidationStatus() {
		return fValidationStatus;
	}
	
	/**
	 * Sets the check passed severity value. This value is used to deceide whether the 
	 * condition check, executed if a change is to be created, is interpreted as passed 
	 * or not. The condition check is considered to be passed if the refactoring status's 
	 * severity is less or equal the given severity. The given value must be smaller
	 * than <code>RefactoringStatus.FATAL</code>.
	 * 
	 * @param severity the severity value considered to be "ok".
	 */
	public void setCheckPassedSeverity(int severity) {
		fCheckPassedSeverity= severity;
		Assert.isTrue (fCheckPassedSeverity < RefactoringStatus.FATAL);
	}
	
	/**
	 * Sets the change context used to execute the change. The given context is passed
	 * to the method <code>IChange.perform</code>.
	 * 
	 * @param context the change context to use
	 * @see IChange#perform(ChangeContext, IProgressMonitor)
	 */
	public void setChangeContext(ChangeContext context) {
		fChangeContext= context;
		Assert.isNotNull(fChangeContext);
	}
	
	/**
	 * Sets the undo manager. If the executed change provides an undo chaneg,
	 * then the undo change is pushed onto this manager.
	 *  
	 * @param manager the undo manager to use or <code>null</code> if no
	 *  undo recording is desired
	 * @param undoName the name used to present the undo change on the undo
	 *  stack. Must be a human-readable string. Must not be <code>null</code>
	 *  if manager is unequal <code>null</code>
	 */
	public void setUndoManager(IUndoManager manager, String undoName) {
		if (manager != null)
			Assert.isNotNull(undoName);
		fUndoManager= manager;
		fUndoName= undoName;
	}
	
	/* (non-Javadoc)
	 * Method declard in IRunnableWithProgress
	 */
	public void run(IProgressMonitor pm) throws InvocationTargetException, InterruptedException {
		try{
			fChangeExecuted= false;
			if (createChange()) {
				pm.beginTask("", 2); //$NON-NLS-1$
				fCreateChangeOperation.run(new SubProgressMonitor(pm, 1, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK));
				fChange= fCreateChangeOperation.getChange();
				RefactoringStatus status= fCreateChangeOperation.getStatus();
				int conditionCheckingStyle= fCreateChangeOperation.getConditionCheckingStyle();
				if (fChange != null && 
						(conditionCheckingStyle == CreateChangeOperation.CHECK_NONE ||
				     	 status != null && status.getSeverity() <= fCheckPassedSeverity)) {
						executeChange(new SubProgressMonitor(pm, 1, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK));
					fChangeExecuted= true;
				} else {
					pm.worked(1);
				}
			} else {
				executeChange(pm);
				fChangeExecuted= true;
			}
		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		} finally{
			pm.done();
		}	
	}
	
	private void executeChange(IProgressMonitor pm) throws CoreException {
		Assert.isNotNull(fChangeContext);
		IRewriteTarget[] targets= null;
		try {
			targets= getRewriteTargets();
			beginCompoundChange(targets);
			// Since we have done precondition checking this check should be fast. No PM.
			// PR: 1GEWDUH: ITPJCORE:WINNT - Refactoring - Unable to undo refactor change
			fChange.aboutToPerform(fChangeContext, new NullProgressMonitor());
			IWorkspaceRunnable runnable= new IWorkspaceRunnable() {
				public void run(IProgressMonitor monitor) throws CoreException {
					Exception exception= null;
					monitor.beginTask("", 10); //$NON-NLS-1$
					try {
						fValidationStatus= fChange.isValid(new SubProgressMonitor(monitor, 1));
						if (fValidationStatus.hasFatalError())
							return;
						if (fUndoManager != null)
							fUndoManager.aboutToPerformChange(fChange);
						fChange.perform(fChangeContext, new SubProgressMonitor(monitor, 9));
					} catch (ChangeAbortException e) {
						exception= e;
						throw new CoreException(
							new Status(
								IStatus.ERROR, 
								JavaPlugin.getPluginId(), IJavaStatusConstants.CHANGE_ABORTED, 
								RefactoringMessages.getString("PerformChangeOperation.unrecoverable_error"), e)); //$NON-NLS-1$
					} catch (CoreException e) {
						exception= e;
						throw e;
					} catch (RuntimeException e) {
						exception= e;
						throw e;
					} finally {
						if (fUndoManager != null) {
							fUndoManager.changePerformed(fChange, exception);
							if (fChange.isUndoable())
								fUndoManager.addUndo(fUndoName, fChange.getUndoChange());
						}
						monitor.done();
					}
				}
			};
			JavaCore.run(runnable, pm);
		} finally {
			fChange.performed();
			if (targets != null)
				endCompoundChange(targets);
		}
	}
	
	private boolean createChange() {
		return fCreateChangeOperation != null;
	}
	
	private static void beginCompoundChange(IRewriteTarget[] targets) {
		for (int i= 0; i < targets.length; i++) {
			targets[i].beginCompoundChange();
		}
	}
	
	private static void endCompoundChange(IRewriteTarget[] targets) {
		for (int i= 0; i < targets.length; i++) {
			targets[i].endCompoundChange();
		}
	}
	
	private static IRewriteTarget[] getRewriteTargets() {
		IEditorPart[] editors= JavaPlugin.getInstanciatedEditors();
		List result= new ArrayList(editors.length);
		for (int i= 0; i < editors.length; i++) {
			IRewriteTarget target= (IRewriteTarget)editors[i].getAdapter(IRewriteTarget.class);
			if (target != null) {
				result.add(target);
			}
		}
		return (IRewriteTarget[]) result.toArray(new IRewriteTarget[result.size()]);
	}
}

