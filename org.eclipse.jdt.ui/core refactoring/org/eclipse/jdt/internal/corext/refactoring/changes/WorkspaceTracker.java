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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.refactoring.ListenerList;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.IUndoManager;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.UndoManagerAdapter;

public class WorkspaceTracker {

	public final static WorkspaceTracker INSTANCE= new WorkspaceTracker();
	
	public interface Listener {
		public void workspaceChanged();
	}
	
	private ListenerList fListeners;
	private JavaModelListener fJavaModelListener;
	private ResourceListener fResourceListener;
	private UndoManagerListener fUndoManagerListener;
	
	private WorkspaceTracker() {
		fListeners= new ListenerList();
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
	
	private class JavaModelListener implements IElementChangedListener {
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
	
	private class ResourceListener implements IResourceChangeListener {
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
	
	private void workspaceChanged() {
		if (fUndoManagerListener != null && fUndoManagerListener.isPerformingChange())
			return;
		Object[] listeners= fListeners.getListeners();
		for (int i= 0; i < listeners.length; i++) {
			((Listener)listeners[i]).workspaceChanged();
		}
	}
	
	public void addListener(Listener l) {
		fListeners.add(l);
		if (fUndoManagerListener == null) {
			fUndoManagerListener= new UndoManagerListener();
			RefactoringCore.getUndoManager().addListener(fUndoManagerListener);
		}
		if (fJavaModelListener == null) {
			fJavaModelListener= new JavaModelListener();
			JavaCore.addElementChangedListener(fJavaModelListener);
		}
		if (fResourceListener == null) {
			fResourceListener= new ResourceListener();
			ResourcesPlugin.getWorkspace().addResourceChangeListener(fResourceListener);
		}
	}
	
	public void removeListener(Listener l) {
		fListeners.remove(l);
		if (fListeners.size() == 0) {
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(fResourceListener);
			fResourceListener= null;
			JavaCore.removeElementChangedListener(fJavaModelListener);
			fJavaModelListener= null;
			RefactoringCore.getUndoManager().removeListener(fUndoManagerListener);
			fUndoManagerListener= null;
		}
	}
}
