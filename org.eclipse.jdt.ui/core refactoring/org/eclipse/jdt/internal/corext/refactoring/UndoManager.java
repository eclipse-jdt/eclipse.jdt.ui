/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.corext.refactoring;

import java.util.Stack;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IWorkspaceRunnable;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.IUndoManager;
import org.eclipse.jdt.internal.corext.refactoring.base.IUndoManagerListener;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;

/**
 * Default implementation of IUndoManager.
 */
public class UndoManager implements IUndoManager {

	private class FlushListener implements IElementChangedListener {
		public void elementChanged(ElementChangedEvent event) {
			// If we don't have anything to undo or redo don't examine the tree.
			if (fUndoChanges.isEmpty() && fRedoChanges.isEmpty())
				return;
			
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
						flush();
						return false;
					}
					break;
				case IJavaElement.COMPILATION_UNIT:
					ICompilationUnit unit= (ICompilationUnit)delta.getElement();
					// If we change a working copy we do nothing
					if (unit.isWorkingCopy()) {
						// Don't examine children of a working copy but keep processing siblings.
						return true;
					} else {
						flush();
						return false;
					}
				case IJavaElement.CLASS_FILE:
					// Don't examine children of a class file but keep on examining siblings.
					return true;
				default:
					flush();
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

	private Stack fUndoChanges;
	private Stack fRedoChanges;
	private Stack fUndoNames;
	private Stack fRedoNames;
	private ListenerList fListeners;
	private FlushListener fFlushListener;
	
	/**
	 * Creates a new undo manager with an empty undo and redo stack.
	 */
	public UndoManager() {
		flush();
	}
	
	/* (Non-Javadoc)
	 * Method declared in IUndoManager.
	 */
	public void addListener(IUndoManagerListener listener) {
		if (fListeners == null)
			fListeners= new ListenerList();
		fListeners.add(listener);
	}
	
	/* (Non-Javadoc)
	 * Method declared in IUndoManager.
	 */
	public void removeListener(IUndoManagerListener listener) {
		if (fListeners == null)
			return;
		fListeners.remove(listener);
	}
	
	/* (Non-Javadoc)
	 * Method declared in IUndoManager.
	 */
	public void aboutToPerformRefactoring() {
		// Remove the resource change listener since we are changing code.
		if (fFlushListener != null)
			JavaCore.removeElementChangedListener(fFlushListener);
	}
	
	/* (Non-Javadoc)
	 * Method declared in IUndoManager.
	 */
	public void refactoringPerformed(boolean success) {
		if (success) {
			if (fFlushListener != null)
				JavaCore.addElementChangedListener(fFlushListener);
		} else {
			flush();
		}
	}
	
	/* (Non-Javadoc)
	 * Method declared in IUndoManager.
	 */
	public void flush(){
		flushUndo();
		flushRedo();
		JavaCore.removeElementChangedListener(fFlushListener);
		fFlushListener= null;
	}
	
	private void flushUndo(){
		fUndoChanges= new Stack();
		fUndoNames= new Stack();
		fireUndoStackChanged();
	}
	
	private void flushRedo(){
		fRedoChanges= new Stack();
		fRedoNames= new Stack();
		fireRedoStackChanged();
	}
		
	/* (Non-Javadoc)
	 * Method declared in IUndoManager.
	 */
	public void addUndo(String refactoringName, IChange change){
		Assert.isNotNull(refactoringName, "refactoring"); //$NON-NLS-1$
		Assert.isNotNull(change, "change"); //$NON-NLS-1$
		fUndoNames.push(refactoringName);
		fUndoChanges.push(change);
		flushRedo();
		if (fFlushListener == null) {
			fFlushListener= new FlushListener();
			JavaCore.addElementChangedListener(fFlushListener);
		}
		fireUndoStackChanged();
	}
	
	/* (Non-Javadoc)
	 * Method declared in IUndoManager.
	 */
	public RefactoringStatus performUndo(ChangeContext context, IProgressMonitor pm) throws JavaModelException{
		// PR: 1GEWDUH: ITPJCORE:WINNT - Refactoring - Unable to undo refactor change
		RefactoringStatus result= new RefactoringStatus();
		
		if (fUndoChanges.empty())
			return result;
			
		IChange change= (IChange)fUndoChanges.peek();
		
		executeChange(result, context, change, pm);
		
		if (!result.hasError()) {
			fUndoChanges.pop();
			fRedoNames.push(fUndoNames.pop());
			fRedoChanges.push(change.getUndoChange());
			fireUndoStackChanged();
			fireRedoStackChanged();
		}
		return result;	
	}

	/* (Non-Javadoc)
	 * Method declared in IUndoManager.
	 */
	public RefactoringStatus performRedo(ChangeContext context, IProgressMonitor pm) throws JavaModelException{
		// PR: 1GEWDUH: ITPJCORE:WINNT - Refactoring - Unable to undo refactor change
		RefactoringStatus result= new RefactoringStatus();

		if (fRedoChanges.empty())
			return result;
			
		IChange change= (IChange)fRedoChanges.peek();
		
		
		executeChange(result, context, change, pm);
		
		if (!result.hasError()) {
			fRedoChanges.pop();
			fUndoNames.push(fRedoNames.pop());
			fUndoChanges.push(change.getUndoChange());
			fireRedoStackChanged();
			fireUndoStackChanged();
		}
		
		return result;
	}

	private void executeChange(RefactoringStatus status, final ChangeContext context, final IChange change, IProgressMonitor pm) throws JavaModelException {
		JavaCore.removeElementChangedListener(fFlushListener);
		try {
			pm.beginTask("", 10); //$NON-NLS-1$
			status.merge(change.aboutToPerform(context, new SubProgressMonitor(pm, 2)));
			if (status.hasError())
				return;
				
			JavaCore.run(
				new IWorkspaceRunnable() {
					public void run(IProgressMonitor innerPM) throws CoreException {
						change.perform(context, innerPM);
					}
				},
				new SubProgressMonitor(pm, 8));
		} catch (CoreException e) {
			throw new JavaModelException(e);
		} finally {
			change.performed();
			JavaCore.addElementChangedListener(fFlushListener);
			pm.done();
		}
	}
	
	/* (Non-Javadoc)
	 * Method declared in IUndoManager.
	 */
	public boolean anythingToRedo(){
		return !fRedoChanges.empty();
	}
	
	/* (Non-Javadoc)
	 * Method declared in IUndoManager.
	 */
	public boolean anythingToUndo(){
		return !fUndoChanges.empty();
	}
	
	/* (Non-Javadoc)
	 * Method declared in IUndoManager.
	 */
	public String peekUndoName() {
		if (fUndoNames.size() > 0)
			return (String)fUndoNames.peek();
		return null;	
	}
	
	/* (Non-Javadoc)
	 * Method declared in IUndoManager.
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
}
