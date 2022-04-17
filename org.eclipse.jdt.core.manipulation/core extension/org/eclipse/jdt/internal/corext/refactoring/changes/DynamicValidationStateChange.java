/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
 *     Microsoft Corporation - copied to jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.changes;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;

//import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;

public class DynamicValidationStateChange extends CompositeChange implements WorkspaceTracker.Listener {

	private boolean fListenerRegistered= false;
	private RefactoringStatus fValidationState= null;
	private long fTimeStamp= DO_NOT_EXPIRE;
	private ISchedulingRule fSchedulingRule;

	private static final long DO_NOT_EXPIRE= -1;

	// 30 minutes
	private static final long LIFE_TIME= 30 * 60 * 1000;

	public DynamicValidationStateChange(Change change) {
		super(change.getName());
		add(change);
		markAsSynthetic();
		fSchedulingRule= ResourcesPlugin.getWorkspace().getRoot();
	}

	public DynamicValidationStateChange(String name) {
		super(name);
		markAsSynthetic();
		fSchedulingRule= ResourcesPlugin.getWorkspace().getRoot();
	}

	public DynamicValidationStateChange(String name, Change[] changes) {
		super(name, changes);
		markAsSynthetic();
		fSchedulingRule= ResourcesPlugin.getWorkspace().getRoot();
	}

	private DynamicValidationStateChange(String name, boolean expire) {
		this(name);
		if (expire) {
			fTimeStamp= 0;
		}
	}

	@Override
	public void initializeValidationData(IProgressMonitor pm) {
		super.initializeValidationData(pm);
		if (fTimeStamp != DO_NOT_EXPIRE) {
			WorkspaceTracker.INSTANCE.addListener(this);
			fListenerRegistered= true;
			fTimeStamp= System.currentTimeMillis();
		}
	}

	@Override
	public void dispose() {
		if (fListenerRegistered) {
			WorkspaceTracker.INSTANCE.removeListener(this);
			fListenerRegistered= false;
		}
		super.dispose();
	}

	@Override
	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
		if (fValidationState == null) {
			return super.isValid(pm);
		}
		return fValidationState;
	}

	@Override
	public Change perform(IProgressMonitor pm) throws CoreException {
		final Change[] result= new Change[1];
		IWorkspaceRunnable runnable= monitor -> result[0]= DynamicValidationStateChange.super.perform(monitor);
		JavaCore.run(runnable, fSchedulingRule, pm);
		return result[0];
	}

	@Override
	protected Change createUndoChange(Change[] childUndos) {
		DynamicValidationStateChange result= new DynamicValidationStateChange(getName(), true);
		for (Change childUndo : childUndos) {
			result.add(childUndo);
		}
		return result;
	}

	@Override
	public void workspaceChanged() {
		long currentTime= System.currentTimeMillis();
		if (currentTime - fTimeStamp < LIFE_TIME)
			return;
		fValidationState= RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.DynamicValidationStateChange_workspace_changed);
		// remove listener from workspace tracker
		WorkspaceTracker.INSTANCE.removeListener(this);
		fListenerRegistered= false;
		// clear up the children to not hang onto too much memory
		for (final Change change : clear()) {
			SafeRunner.run(new ISafeRunnable() {
				@Override
				public void run() throws Exception {
					change.dispose();
				}
				@Override
				public void handleException(Throwable exception) {
					JavaManipulationPlugin.log(exception);
				}
			});
		}
	}

	public void setSchedulingRule(ISchedulingRule schedulingRule) {
		fSchedulingRule= schedulingRule;
	}

	public ISchedulingRule getSchedulingRule() {
		return fSchedulingRule;
	}

	@Override
	public String toString() {
		return String.valueOf(this.getFilenumber());
	}
}
