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

package org.eclipse.jdt.internal.corext.refactoring;

import java.util.Collection;
import java.util.Iterator;
import java.util.Stack;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IWorkspaceRunnable;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.IUndoManager;
import org.eclipse.jdt.internal.corext.refactoring.base.IUndoManagerListener;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.ltk.refactoring.core.IDynamicValidationStateChange;
import org.eclipse.ltk.refactoring.core.IValidationStateListener;
import org.eclipse.ltk.refactoring.core.ValidationStateChangedEvent;

/**
 * Default implementation of IUndoManager.
 */
public class UndoManager implements IUndoManager {

	private class ValidationStateListener implements IValidationStateListener {
		public void stateChanged(ValidationStateChangedEvent event) {
			validationStateChanged(event.getChange());
		}
	}

	private Stack fUndoChanges;
	private Stack fRedoChanges;
	private Stack fUndoNames;
	private Stack fRedoNames;

	private ListenerList fListeners;

	private ValidationStateListener fValidationListener;

	/**
	 * Creates a new undo manager with an empty undo and redo stack.
	 */
	public UndoManager() {
		flush();
		fValidationListener= new ValidationStateListener();
	}

	/*
	 * (Non-Javadoc) Method declared in IUndoManager.
	 */
	public void addListener(IUndoManagerListener listener) {
		if (fListeners == null)
			fListeners= new ListenerList();
		fListeners.add(listener);
	}

	/*
	 * (Non-Javadoc) Method declared in IUndoManager.
	 */
	public void removeListener(IUndoManagerListener listener) {
		if (fListeners == null)
			return;
		fListeners.remove(listener);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void aboutToPerformChange(IChange change) {
		sendAboutToPerformChange(fUndoChanges, change);
		sendAboutToPerformChange(fRedoChanges, change);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void changePerformed(IChange change, Exception e) {
		sendChangePerformed(fUndoChanges, change, e);
		sendChangePerformed(fRedoChanges, change, e);
	}

	/*
	 * (Non-Javadoc) Method declared in IUndoManager.
	 */
	public void aboutToPerformRefactoring() {
	}

	/*
	 * (Non-Javadoc) Method declared in IUndoManager.
	 */
	public void refactoringPerformed(boolean success) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see IUndoManager#shutdown()
	 */
	public void shutdown() {
	}

	/*
	 * (Non-Javadoc) Method declared in IUndoManager.
	 */
	public void flush() {
		flushUndo();
		flushRedo();
	}

	private void flushUndo() {
		if (fUndoChanges != null) {
			removeValidationStateListener(fUndoChanges);
		}
		fUndoChanges= new Stack();
		fUndoNames= new Stack();
		fireUndoStackChanged();
	}

	private void flushRedo() {
		if (fRedoChanges != null) {
			removeValidationStateListener(fRedoChanges);
		}
		fRedoChanges= new Stack();
		fRedoNames= new Stack();
		fireRedoStackChanged();
	}

	/*
	 * (Non-Javadoc) Method declared in IUndoManager.
	 */
	public void addUndo(String refactoringName, IChange change) {
		Assert.isNotNull(refactoringName, "refactoring"); //$NON-NLS-1$
		Assert.isNotNull(change, "change"); //$NON-NLS-1$
		fUndoNames.push(refactoringName);
		fUndoChanges.push(change);
		flushRedo();
		addValidationStateListener(change);
		fireUndoStackChanged();
	}

	/*
	 * (Non-Javadoc) Method declared in IUndoManager.
	 */
	public RefactoringStatus performUndo(ChangeContext context, IProgressMonitor pm) throws CoreException {
		RefactoringStatus result= new RefactoringStatus();

		if (fUndoChanges.empty())
			return result;

		IChange change= (IChange)fUndoChanges.pop();
		removeValidationStateListener(change);

		executeChange(result, context, change, pm);

		if (!result.hasError()) {
			IChange redo= null;
			if (change.isUndoable() && (redo= change.getUndoChange()) != null && !fUndoNames.isEmpty()) {
				addValidationStateListener(redo);
				fRedoNames.push(fUndoNames.pop());
				fRedoChanges.push(redo);
				fireUndoStackChanged();
				fireRedoStackChanged();
			} else {
				flush();
			}
		} else {
			flush();
		}
		return result;
	}

	/*
	 * (Non-Javadoc) Method declared in IUndoManager.
	 */
	public RefactoringStatus performRedo(ChangeContext context, IProgressMonitor pm) throws CoreException {
		// PR: 1GEWDUH: ITPJCORE:WINNT - Refactoring - Unable to undo refactor change
		RefactoringStatus result= new RefactoringStatus();

		if (fRedoChanges.empty())
			return result;

		IChange change= (IChange)fRedoChanges.pop();
		removeValidationStateListener(change);

		executeChange(result, context, change, pm);

		if (!result.hasError()) {
			IChange undo= null;
			if (change.isUndoable() && (undo= change.getUndoChange()) != null && !fRedoNames.isEmpty()) {
				addValidationStateListener(undo);
				fUndoNames.push(fRedoNames.pop());
				fUndoChanges.push(undo);
				fireRedoStackChanged();
				fireUndoStackChanged();
			}
		} else {
			flush();
		}

		return result;
	}

	private void executeChange(RefactoringStatus status, final ChangeContext context, final IChange change, IProgressMonitor pm) throws CoreException {
		Exception exception= null;
		try {
			pm.beginTask("", 10); //$NON-NLS-1$
			status.merge(change.aboutToPerform(context, new SubProgressMonitor(pm, 2)));
			if (status.hasError())
				return;
			status.merge(change.isValid(new SubProgressMonitor(pm, 2)));
			if (status.hasFatalError())
				return;

			aboutToPerformChange(change);
			JavaCore.run(new IWorkspaceRunnable() {
				public void run(IProgressMonitor innerPM) throws CoreException {
					change.perform(context, innerPM);
				}
			}, new SubProgressMonitor(pm, 8));
		} catch (RuntimeException e) {
			exception= e;
			throw e;
		} catch (CoreException e) {
			exception= e;
			throw e;
		} finally {
			changePerformed(change, exception);
			change.performed();
			pm.done();
		}
	}

	/*
	 * (Non-Javadoc) Method declared in IUndoManager.
	 */
	public boolean anythingToRedo() {
		return !fRedoChanges.empty();
	}

	/*
	 * (Non-Javadoc) Method declared in IUndoManager.
	 */
	public boolean anythingToUndo() {
		return !fUndoChanges.empty();
	}

	/*
	 * (Non-Javadoc) Method declared in IUndoManager.
	 */
	public String peekUndoName() {
		if (fUndoNames.size() > 0)
			return (String)fUndoNames.peek();
		return null;
	}

	/*
	 * (Non-Javadoc) Method declared in IUndoManager.
	 */
	public String peekRedoName() {
		if (fRedoNames.size() > 0)
			return (String)fRedoNames.peek();
		return null;
	}

	private void fireUndoStackChanged() {
		if (fListeners == null)
			return;
		Object[] listeners= fListeners.getListeners();
		for (int i= 0; i < listeners.length; i++) {
			((IUndoManagerListener)listeners[i]).undoStackChanged(this);
		}
	}

	private void fireRedoStackChanged() {
		if (fListeners == null)
			return;
		Object[] listeners= fListeners.getListeners();
		for (int i= 0; i < listeners.length; i++) {
			((IUndoManagerListener)listeners[i]).redoStackChanged(this);
		}
	}
	
	private void sendAboutToPerformChange(Collection collection, IChange change) {
		for (Iterator iter= collection.iterator(); iter.hasNext();) {
			Object element= iter.next();
			if (element instanceof IDynamicValidationStateChange) {
				((IDynamicValidationStateChange)element).aboutToPerformChange(change);
			}
			
		}
	}
	
	private void sendChangePerformed(Collection collection, IChange change, Exception e) {
		for (Iterator iter= collection.iterator(); iter.hasNext();) {
			Object element= iter.next();
			if (element instanceof IDynamicValidationStateChange) {
				((IDynamicValidationStateChange)element).changePerformed(change, e);
			}
			
		}
	}
	
	private void removeValidationStateListener(IChange change) {
		if (change instanceof IDynamicValidationStateChange)
			((IDynamicValidationStateChange)change).removeValidationStateListener(fValidationListener);
	}
	
	private void addValidationStateListener(IChange change) {
		if (change instanceof IDynamicValidationStateChange)
			((IDynamicValidationStateChange)change).addValidationStateListener(fValidationListener);
	}
	
	private void removeValidationStateListener(Collection collection) {
		for (Iterator iter= collection.iterator(); iter.hasNext();) {
			Object element= iter.next();
			if (element instanceof IDynamicValidationStateChange) {
				((IDynamicValidationStateChange)element).removeValidationStateListener(fValidationListener);
			}
			
		}
	}

	private void validationStateChanged(IChange change) {
		try {
			if (!change.isValid(new NullProgressMonitor()).isOK())
				flush();
		} catch (CoreException e) {
			flush();
		}
	}
}
