/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */
package org.eclipse.jdt.internal.core.refactoring;

import java.util.Stack;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.refactoring.IChange;
import org.eclipse.jdt.core.refactoring.IUndoManager;
import org.eclipse.jdt.core.refactoring.IUndoManagerListener;

/**
 * Default implementation of IUndoManager.
 */
public class UndoManager implements IUndoManager {

	private class FlushListener implements IElementChangedListener {
		public void elementChanged(ElementChangedEvent event) {
			flush();
		}
	}

	private Stack fUndoChanges;
	private Stack fRedoChanges;
	private Stack fUndoNames;
	private Stack fRedoNames;
	private ListenerList fListeners;
	private FlushListener fFlushListener;
	
	public UndoManager(){
		flush();
	}
	
	public void addListener(IUndoManagerListener listener) {
		if (fListeners == null)
			fListeners= new ListenerList();
		fListeners.add(listener);
	}
	
	public void removeListener(IUndoManagerListener listener) {
		if (fListeners == null)
			return;
		fListeners.remove(listener);
	}
	
	public void flush(){
		flushUndo();
		flushRedo();
		JavaCore.removeElementChangedListener(fFlushListener);
		fFlushListener= null;
	}
	
	private void flushUndo(){
		fUndoChanges= new Stack();
		fUndoNames= new Stack();
		fireNoMoreUndos();
	}
	
	private void flushRedo(){
		fRedoChanges= new Stack();
		fRedoNames= new Stack();
		fireNoMoreRedos();
	}
		
	public void addUndo(String refactoringName, IChange change){
		Assert.isNotNull(refactoringName, "refactoring must not be null here");
		Assert.isNotNull(change, "change must not be null here");
		fUndoNames.push(refactoringName);
		fUndoChanges.push(change);
		flushRedo();
		if (fFlushListener == null) {
			fFlushListener= new FlushListener();
			JavaCore.addElementChangedListener(fFlushListener);
		}
		fireUndoAdded();
	}
	
	public void performUndo(IProgressMonitor pm) throws JavaModelException{
		//maybe i should not check that and just let it fail
		if (fUndoChanges.empty())
			return;
			
		IChange change= (IChange)fUndoChanges.pop();
		if (fUndoChanges.size() == 0)
			fireNoMoreUndos();
		fRedoNames.push(fUndoNames.pop());
		
		executeChange(change, pm);
		
		fRedoChanges.push(change.getUndoChange());
		fireRedoAdded();
	}

	public void performRedo(IProgressMonitor pm) throws JavaModelException{
		//maybe i should not check that and just let it fail
		if (fRedoChanges.empty())
			return;
			
		IChange change= (IChange)fRedoChanges.pop();
		if (fRedoChanges.size() == 0)
			fireNoMoreRedos();	
		fUndoNames.push(fRedoNames.pop());
		
		executeChange(change, pm);
		
		fUndoChanges.push(change.getUndoChange());
		fireUndoAdded();
	}

	private void executeChange(final IChange change, IProgressMonitor pm) throws JavaModelException {
		JavaCore.removeElementChangedListener(fFlushListener);
		try {
			change.aboutToPerform();
			IWorkspace workspace= ResourcesPlugin.getWorkspace();
			workspace.run(
				new IWorkspaceRunnable() {
					public void run(IProgressMonitor pm) throws CoreException {
						try {
							change.perform(pm);
						} catch (JavaModelException e) {
							throw new CoreException(e.getStatus());
						}
					}
				},
				pm);
		} catch (CoreException e) {
			throw new JavaModelException(e);
		} finally {
			change.performed();
			JavaCore.addElementChangedListener(fFlushListener);
		}
	}
	
	public boolean anythingToRedo(){
		return !fRedoChanges.empty();
	}
	
	public boolean anythingToUndo(){
		return !fUndoChanges.empty();
	}
	
	public String peekUndoName() {
		if (fUndoNames.size() > 0)
			return (String)fUndoNames.peek();
		return null;	
	}
	
	public String peekRedoName() {
		if (fRedoNames.size() > 0)
			return (String)fRedoNames.peek();
		return null;	
	}
	
	private void fireUndoAdded() {
		if (fListeners == null)
			return;
		Object[] listeners= fListeners.getListeners();
		for (int i= 0; i < listeners.length; i++) {
			((IUndoManagerListener)listeners[i]).undoAdded();
		}
	}
	
	private void fireNoMoreUndos() {
		if (fListeners == null)
			return;
		Object[] listeners= fListeners.getListeners();
		for (int i= 0; i < listeners.length; i++) {
			((IUndoManagerListener)listeners[i]).noMoreUndos();
		}
	}
	
	private void fireRedoAdded() {
		if (fListeners == null)
			return;
		Object[] listeners= fListeners.getListeners();
		for (int i= 0; i < listeners.length; i++) {
			((IUndoManagerListener)listeners[i]).redoAdded();
		}
	}
	
	private void fireNoMoreRedos() {
		if (fListeners == null)
			return;
		Object[] listeners= fListeners.getListeners();
		for (int i= 0; i < listeners.length; i++) {
			((IUndoManagerListener)listeners[i]).noMoreRedos();
		}
	}	
}