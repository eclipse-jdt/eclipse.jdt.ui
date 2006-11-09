/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IThreadListener;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.internal.ui.refactoring.ChangeExceptionHandler;
import org.eclipse.ltk.internal.ui.refactoring.RefactoringWizardDialog2;
import org.eclipse.ltk.ui.refactoring.RefactoringUI;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jdt.internal.ui.refactoring.reorg.RenameRefactoringWizard;

/**
 * A helper class to execute a refactoring. The class takes care of pushing the
 * undo change onto the undo stack and folding editor edits into one editor
 * undo object.
 */
public class RefactoringExecutionHelper {

	private final Refactoring fRefactoring;
	private final Shell fParent;
	private final IRunnableContext fExecContext;
	private final int fStopSeverity;
	private final boolean fNeedsSavedEditors;

	private class Operation implements IWorkspaceRunnable {
		public Change fChange;
		public PerformChangeOperation fPerformChangeOperation;
		private final boolean fShowPreview;
		private final boolean fApplyChanges;
		
		public Operation(boolean showPreview, boolean applyChanges) {
			fShowPreview= showPreview;
			fApplyChanges= applyChanges;
        }
		
		public void run(IProgressMonitor pm) throws CoreException {
			try {
				pm.beginTask("", fApplyChanges?11:7); //$NON-NLS-1$
				pm.subTask(""); //$NON-NLS-1$
				
				RefactoringStatus status= fRefactoring.checkAllConditions(new SubProgressMonitor(pm, 4, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK));
				int severity= status.getSeverity();
				if (fShowPreview) {
					if (severity == RefactoringStatus.FATAL) {
						showStatusDialog(status);
					}
				} else if (severity >= fStopSeverity) {
					showStatusDialog(status);
				}

				fChange= fRefactoring.createChange(new SubProgressMonitor(pm, 2, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK));
				fChange.initializeValidationData(new SubProgressMonitor(pm, 1, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK));
				if (fShowPreview) {
					RenameRefactoringWizard wizard= new RenameRefactoringWizard(fRefactoring, fRefactoring.getName(), null, null, null) {
						protected void addUserInputPages() {
							// nothing to add
						}
					};
					RefactoringWizardDialog2 dialog= new RefactoringWizardDialog2(fParent, wizard);
					if (dialog.open() == IDialogConstants.CANCEL_ID) {
						throw new OperationCanceledException();
					} else {
						return;
					}
				}
				
				fPerformChangeOperation= RefactoringUI.createUIAwareChangeOperation(fChange);
				fPerformChangeOperation.setUndoManager(RefactoringCore.getUndoManager(), fRefactoring.getName());
				if (fRefactoring instanceof IScheduledRefactoring)
					fPerformChangeOperation.setSchedulingRule(((IScheduledRefactoring)fRefactoring).getSchedulingRule());
				
				if (fApplyChanges)
					fPerformChangeOperation.run(new SubProgressMonitor(pm, 4, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK));
			} finally {
				pm.done();
			}
		}

		private void showStatusDialog(RefactoringStatus status) throws OperationCanceledException {
			Dialog dialog= RefactoringUI.createRefactoringStatusDialog(status, fParent, fRefactoring.getName(), false);
			if (dialog.open() == IDialogConstants.CANCEL_ID) {
				throw new OperationCanceledException();
			}
		}
	}
	
	public RefactoringExecutionHelper(Refactoring refactoring, int stopSevertity, boolean needsSavedEditors, Shell parent, IRunnableContext context) {
		super();
		Assert.isNotNull(refactoring);
		Assert.isNotNull(parent);
		Assert.isNotNull(context);
		fRefactoring= refactoring;
		fStopSeverity= stopSevertity;
		fParent= parent;
		fExecContext= context;
		fNeedsSavedEditors= needsSavedEditors;
	}
	
	public void perform(boolean fork, boolean cancelable) throws InterruptedException, InvocationTargetException {
		perform(false, fork, cancelable);
	}
	
	public void perform(boolean showPreview, boolean fork, boolean cancelable) throws InterruptedException, InvocationTargetException {
		Assert.isTrue(Display.getCurrent() != null);
		final IJobManager manager=  Job.getJobManager();
		final ISchedulingRule rule;
		if (fRefactoring instanceof IScheduledRefactoring) {
			rule= ((IScheduledRefactoring)fRefactoring).getSchedulingRule();
		} else {
			rule= ResourcesPlugin.getWorkspace().getRoot();
		}
		class OperationRunner extends WorkbenchRunnableAdapter implements IThreadListener {
			public OperationRunner(IWorkspaceRunnable runnable, ISchedulingRule schedulingRule) {
				super(runnable, schedulingRule);
			}
			public void threadChange(Thread thread) {
				manager.transferRule(getSchedulingRule(), thread);
			}
		}
		try {
			try {
				Runnable r= new Runnable() {
					public void run() {
						manager.beginRule(rule, null);
					}
				};
				BusyIndicator.showWhile(fParent.getDisplay(), r);
			} catch (OperationCanceledException e) {
				throw new InterruptedException(e.getMessage());
			}
			
			RefactoringSaveHelper saveHelper= new RefactoringSaveHelper();
			if (fNeedsSavedEditors && !saveHelper.saveEditors(fParent))
				throw new InterruptedException();
			final Operation op= new Operation(showPreview, !fork);
			fRefactoring.setValidationContext(fParent);
			try{
				fExecContext.run(fork, cancelable, new OperationRunner(op, rule));
				if (fork && op.fPerformChangeOperation != null)
					fExecContext.run(false, false, new OperationRunner(op.fPerformChangeOperation, rule));

				if (op.fPerformChangeOperation != null) {
					RefactoringStatus validationStatus= op.fPerformChangeOperation.getValidationStatus();
					if (validationStatus != null && validationStatus.hasFatalError()) {
						MessageDialog.openError(fParent, fRefactoring.getName(), 
								Messages.format(
										RefactoringMessages.RefactoringExecutionHelper_cannot_execute, 
										validationStatus.getMessageMatchingSeverity(RefactoringStatus.FATAL)));
						return;
					}
				}
			} catch (InvocationTargetException e) {
				PerformChangeOperation pco= op.fPerformChangeOperation;
				if (pco != null && pco.changeExecutionFailed()) {
					ChangeExceptionHandler handler= new ChangeExceptionHandler(fParent, fRefactoring);
					Throwable inner= e.getTargetException();
					if (inner instanceof RuntimeException) {
						handler.handle(pco.getChange(), (RuntimeException)inner);
					} else if (inner instanceof CoreException) {
						handler.handle(pco.getChange(), (CoreException)inner);
					} else {
						throw e;
					}
				} else {
					throw e;
				}
			}catch (OperationCanceledException e) {
				throw new InterruptedException(e.getMessage());
			} finally {
				saveHelper.triggerBuild();
			}
		} finally {
			manager.endRule(rule);
			fRefactoring.setValidationContext(null);
		}
	}	
}
