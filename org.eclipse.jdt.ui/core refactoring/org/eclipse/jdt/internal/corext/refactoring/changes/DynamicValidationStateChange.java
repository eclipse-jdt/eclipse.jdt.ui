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
package org.eclipse.jdt.internal.corext.refactoring.changes;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IWorkspaceRunnable;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class DynamicValidationStateChange extends CompositeChange implements WorkspaceTracker.Listener {
	
	private RefactoringStatus fValidationState= null;
	
	public DynamicValidationStateChange(Change change) {
		super(change.getName());
		add(change);
		markAsSynthetic();
	}
	
	public DynamicValidationStateChange(String name) {
		super(name);
		markAsSynthetic();
	}
	
	public DynamicValidationStateChange(String name, Change[] changes) {
		super(name, changes);
		markAsSynthetic();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void initializeValidationData(IProgressMonitor pm) {
		super.initializeValidationData(pm);
		WorkspaceTracker.INSTANCE.addListener(this);
	}
	
	public void dispose() {
		WorkspaceTracker.INSTANCE.removeListener(this);
		super.dispose();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
		if (fValidationState == null) {
			return super.isValid(pm);
		}
		return fValidationState;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Change perform(IProgressMonitor pm) throws CoreException {
		final Change[] result= new Change[1];
		IWorkspaceRunnable runnable= new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				result[0]= DynamicValidationStateChange.super.perform(monitor);
			}
		};
		JavaCore.run(runnable, pm);
		return result[0];
	}

	/**
	 * {@inheritDoc}
	 */
	protected Change createUndoChange(Change[] childUndos) {
		DynamicValidationStateChange result= new DynamicValidationStateChange(getName());
		for (int i= 0; i < childUndos.length; i++) {
			result.add(childUndos[i]);
		}
		return result;
	}
	
	public void workspaceChanged() {
		fValidationState= RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("DynamicValidationStateChange.workspace_changed")); //$NON-NLS-1$
		RefactoringCore.getUndoManager().flush();
	}
}
