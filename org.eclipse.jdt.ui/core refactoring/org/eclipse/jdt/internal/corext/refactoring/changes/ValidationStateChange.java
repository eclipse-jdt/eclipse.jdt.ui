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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.IUndoManager;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.UndoManagerAdapter;

public class ValidationStateChange extends CompositeChange {
	
	private class FlushListener implements IElementChangedListener {
		public void elementChanged(ElementChangedEvent event) {
			processDelta(event.getDelta());				
		}
		private boolean processDelta(IJavaElementDelta delta) {
			int kind= delta.getKind();
			int details= delta.getFlags();
			int type= delta.getElement().getElementType();
			
			switch (type) {
				// Consider containers for class files.
				case IJavaElement.JAVA_MODEL:
				case IJavaElement.JAVA_PROJECT:
				case IJavaElement.PACKAGE_FRAGMENT_ROOT:
				case IJavaElement.PACKAGE_FRAGMENT:
					// If we did some different than changing a child we flush the the undo / redo stack.
					if (kind != IJavaElementDelta.CHANGED || details != IJavaElementDelta.F_CHILDREN) {
						workspaceChanged();
						return false;
					}
					break;
				case IJavaElement.COMPILATION_UNIT:
					// if we have changed a primary working copy (e.g created, removed, ...)
					// then we do nothing.
					if ((details & IJavaElementDelta.F_PRIMARY_WORKING_COPY) != 0) {
						return true;
					}
					ICompilationUnit unit= (ICompilationUnit)delta.getElement();
					// If we change a working copy we do nothing
					if (unit.isWorkingCopy()) {
						// Don't examine children of a working copy but keep processing siblings.
						return true;
					} else {
						workspaceChanged();
						return false;
					}
				case IJavaElement.CLASS_FILE:
					// Don't examine children of a class file but keep on examining siblings.
					return true;
				default:
					workspaceChanged();
					return false;	
			}
				
			IJavaElementDelta[] affectedChildren= delta.getAffectedChildren();
			if (affectedChildren == null)
				return true;
	
			for (int i= 0; i < affectedChildren.length; i++) {
				if (!processDelta(affectedChildren[i]))
					return false;
			}
			return true;			
		}
	}
	
	private class SaveListener implements IResourceChangeListener {
		public void resourceChanged(IResourceChangeEvent event) {
			IResourceDeltaVisitor visitor= new IResourceDeltaVisitor() {
				public boolean visit(IResourceDelta delta) throws CoreException {
					IResource resource= delta.getResource();
					if (resource.getType() == IResource.FILE && delta.getKind() == IResourceDelta.CHANGED &&
							(delta.getFlags() & IResourceDelta.CONTENT) != 0) {
						String ext= ((IFile)resource).getFileExtension();
						if (ext != null && "java".equals(ext)) { //$NON-NLS-1$
							ICompilationUnit unit= JavaCore.createCompilationUnitFrom((IFile)resource);
							if (unit != null && unit.exists()) {
								workspaceChanged();
								return false;
							}
						}
					}
					return true;
				}
			};
			try {
				IResourceDelta delta= event.getDelta();
				if (delta != null)
					delta.accept(visitor);
			} catch (CoreException e) {
				JavaPlugin.log(e.getStatus());
			}
		}
	}
	
	private class UndoManagerListener extends UndoManagerAdapter {
		private int fInRefactoringCount;
		
		public void aboutToPerformChange(IUndoManager manager, Change change) {
			fInRefactoringCount++;
		}
		public void changePerformed(IUndoManager manager, Change change) {
			fInRefactoringCount--;
		}
		public boolean isPerformingChange() {
			return fInRefactoringCount > 0;
		}
	}
	
	private RefactoringStatus fValidationState= null;
	
	private FlushListener fFlushListener;
	private SaveListener fSaveListener;
	private UndoManagerListener fUndoManagerListener;
	
	public ValidationStateChange() {
		super();
		markAsGeneric();
	}
	
	public ValidationStateChange(Change change) {
		super(change.getName());
		add(change);
		markAsGeneric();
	}
	
	public ValidationStateChange(String name) {
		super(name);
		markAsGeneric();
	}
	
	public ValidationStateChange(String name, Change[] changes) {
		super(name, changes);
		markAsGeneric();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void initializeValidationData(IProgressMonitor pm) {
		super.initializeValidationData(pm);
		addListeners();
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
				result[0]= ValidationStateChange.super.perform(monitor);
			}
		};
		JavaCore.run(runnable, pm);
		return result[0];
	}

	/**
	 * {@inheritDoc}
	 */
	protected Change createUndoChange(Change[] childUndos) {
		ValidationStateChange result= new ValidationStateChange();
		for (int i= 0; i < childUndos.length; i++) {
			result.add(childUndos[i]);
		}
		return result;
	}
	
	public void dispose() {
		removeListeners();
		super.dispose();
	}
	
	private void addListeners() {
		if (fUndoManagerListener == null) {
			fUndoManagerListener= new UndoManagerListener();
			RefactoringCore.getUndoManager().addListener(fUndoManagerListener);
		}
		if (fFlushListener == null) {
			fFlushListener= new FlushListener();
			JavaCore.addElementChangedListener(fFlushListener);
		}
		if (fSaveListener == null) {
			fSaveListener= new SaveListener();
			ResourcesPlugin.getWorkspace().addResourceChangeListener(fSaveListener);
		}
	}

	private void removeListeners() {
		if (fSaveListener != null) {
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(fSaveListener);
			fSaveListener= null;
		}
		if (fFlushListener != null) {
			JavaCore.removeElementChangedListener(fFlushListener);
			fFlushListener= null;
		}
		if (fUndoManagerListener != null) {
			RefactoringCore.getUndoManager().removeListener(fUndoManagerListener);
			fUndoManagerListener= null;
		}
	}

	private void workspaceChanged() {
		if (fUndoManagerListener != null && fUndoManagerListener.isPerformingChange())
			return;
		fValidationState= RefactoringStatus.createFatalErrorStatus("Workspace has changed since change has been created");
		RefactoringCore.getUndoManager().flush();
		removeListeners();
	}
}
