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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.util.Assert;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.internal.corext.refactoring.base.Change;
import org.eclipse.jdt.internal.corext.refactoring.base.IUndoManager;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Operation that, when performed, performes a change to the workbench.
 */
public class PerformChangeOperation implements IWorkspaceRunnable {

	private Change fChange;
	private CreateChangeOperation fCreateChangeOperation;
	private int fCheckPassedSeverity;
	private boolean fChangeExecuted;
	private RefactoringStatus fValidationStatus;
	private Change fUndoChange;
	
	private String fUndoName;
	private IUndoManager fUndoManager;
	
	private boolean fChangeExecutionFailed;
	
	/**
	 * Creates a new perform change operation instance for the given change.
	 * 
	 * @param change the change to be applied to the workbench
	 */
	public PerformChangeOperation(Change change) {
		fChange= change;
		Assert.isNotNull(fChange);
	}

	/**
	 * Creates a new <code>PerformChangeOperation</code> for the given {@link 
	 * CreateChangeOperation}. The create change operation is used to create 
	 * the actual change to execute.
	 * 
	 * @param op the <code>CreateChangeOperation</code> used to create the
	 *  actual change object
	 */
	public PerformChangeOperation(CreateChangeOperation op) {
		fCreateChangeOperation= op;
		Assert.isNotNull(fCreateChangeOperation);
		fCheckPassedSeverity= op.getCheckPassedSeverity();
	}
	
	public boolean getChangeExecutionFailed() {
		return fChangeExecutionFailed;
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
	public Change getChange() {
		return fChange;
	}
	
	/**
	 * Returns the undo change of the change performed by this operation. Returns
	 * <code>null</code> if the change hasn't been performed or if the change
	 * doesn't provide a undo.
	 * 
	 * @return the undo change of the performed change or <code>null</code>
	 */
	public Change getUndoChange() {
		return fUndoChange;
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
		if (manager != null) {
			Assert.isNotNull(undoName);
		}
		fUndoManager= manager;
		fUndoName= undoName;
	}
	
	/* (non-Javadoc)
	 * Method declard in IRunnableWithProgress
	 */
	public void run(IProgressMonitor pm) throws CoreException {
		try {
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
				} else {
					pm.worked(1);
				}
			} else {
				executeChange(pm);
			}
		} finally {
			pm.done();
		}	
	}
	
	private void executeChange(IProgressMonitor pm) throws CoreException {
		if (!fChange.isEnabled())
			return;
		IRewriteTarget[] targets= null;
		try {
			targets= getRewriteTargets();
			beginCompoundChange(targets);
			IWorkspaceRunnable runnable= new IWorkspaceRunnable() {
				public void run(IProgressMonitor monitor) throws CoreException {
					monitor.beginTask("", 10); //$NON-NLS-1$
					Exception exception= null;
					fValidationStatus= fChange.isValid(new SubProgressMonitor(monitor, 1));
					if (fValidationStatus.hasFatalError())
						return; //TODO: inform user about error
					try {
						if (fUndoManager != null) {
							ResourcesPlugin.getWorkspace().checkpoint(false);
							fUndoManager.aboutToPerformChange(fChange);
						}
						fChangeExecutionFailed= true;
						fChangeExecuted= true;
						fUndoChange= fChange.perform(new SubProgressMonitor(monitor, 9));
						fChangeExecutionFailed= false;
						if (fUndoChange != null)
							fUndoChange.initializeValidationData(new SubProgressMonitor(monitor, 1));
					} catch (CoreException e) {
						exception= e;
						throw e;
					} catch (RuntimeException e) {
						exception= e;
						throw e;
					} finally {
						try {
							fChange.dispose();
							if (fUndoManager != null) {
								ResourcesPlugin.getWorkspace().checkpoint(false);
								fUndoManager.changePerformed(fChange, fUndoChange, exception);
								if (fUndoChange != null) {
									fUndoManager.addUndo(fUndoName, fUndoChange);
								} else {
									fUndoManager.flush();
								}
							}
						} catch (RuntimeException e) {
							fUndoManager.flush();
							throw e;
						}
						monitor.done();
					}
				}
			};
			JavaCore.run(runnable, pm);
		} finally {
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

