/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring;

import java.util.Stack;import org.eclipse.core.resources.IWorkspace;import org.eclipse.core.resources.IWorkspaceRunnable;import org.eclipse.core.resources.ResourcesPlugin;import org.eclipse.core.runtime.CoreException;import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.core.runtime.SubProgressMonitor;import org.eclipse.jdt.core.ElementChangedEvent;import org.eclipse.jdt.core.IElementChangedListener;import org.eclipse.jdt.core.JavaCore;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.internal.core.refactoring.base.ChangeContext;import org.eclipse.jdt.internal.core.refactoring.base.IChange;import org.eclipse.jdt.internal.core.refactoring.base.IUndoManager;import org.eclipse.jdt.internal.core.refactoring.base.IUndoManagerListener;import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;

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
	
	public RefactoringStatus performUndo(ChangeContext context, IProgressMonitor pm) throws JavaModelException{
		// PR: 1GEWDUH: ITPJCORE:WINNT - Refactoring - Unable to undo refactor change
		RefactoringStatus result= new RefactoringStatus();
		
		//XXX: maybe i should not check that and just let it fail
		if (fUndoChanges.empty())
			return result;
			
		IChange change= (IChange)fUndoChanges.peek();
		
		executeChange(result, context, change, pm);
		
		if (!result.hasError()) {
			fUndoChanges.pop();
			if (fUndoChanges.size() == 0)
				fireNoMoreUndos();
			fRedoNames.push(fUndoNames.pop());
			fRedoChanges.push(change.getUndoChange());
			fireRedoAdded();
		}
		return result;	
	}

	public RefactoringStatus performRedo(ChangeContext context, IProgressMonitor pm) throws JavaModelException{
		// PR: 1GEWDUH: ITPJCORE:WINNT - Refactoring - Unable to undo refactor change
		RefactoringStatus result= new RefactoringStatus();
		//XXX: maybe i should not check that and just let it fail
		if (fRedoChanges.empty())
			return result;
			
		IChange change= (IChange)fRedoChanges.peek();
		
		
		executeChange(result, context, change, pm);
		
		if (!result.hasError()) {
			fRedoChanges.pop();
			if (fRedoChanges.size() == 0)
				fireNoMoreRedos();	
			fUndoNames.push(fRedoNames.pop());
			fUndoChanges.push(change.getUndoChange());
			fireUndoAdded();
		}
		
		return result;
	}

	private void executeChange(RefactoringStatus status, final ChangeContext context, final IChange change, IProgressMonitor pm) throws JavaModelException {
		JavaCore.removeElementChangedListener(fFlushListener);
		try {
			pm.beginTask("", 10);
			status.merge(change.aboutToPerform(context, new SubProgressMonitor(pm, 2)));
			if (status.hasError())
				return;
				
			IWorkspace workspace= ResourcesPlugin.getWorkspace();
			workspace.run(
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