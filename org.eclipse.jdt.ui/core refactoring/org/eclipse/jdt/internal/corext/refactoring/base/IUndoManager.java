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
package org.eclipse.jdt.internal.corext.refactoring.base;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * An undo manager keeps track of changes performed by refactorings. Use <code>performUndo</code> 
 * and <code>performRedo</code> to undo and redo changes.
 * <p>
 * NOTE: This interface is not intended to be implemented or extended. Use Refactoring.getUndoManager()
 * to access the undo manager. </p>
 * <p>
 * <bf>NOTE:<bf> This class/interface is part of an interim API that is still under development 
 * and expected to change significantly before reaching stability. It is being made available at 
 * this early stage to solicit feedback from pioneering adopters on the understanding that any 
 * code that uses this API will almost certainly be broken (repeatedly) as the API evolves.</p>
 */
public interface IUndoManager {

	/**
	 * Adds a listener to the undo manager.
	 * 
	 * @param listener the listener to be added to the undo manager
	 */
	public void addListener(IUndoManagerListener listener);
	
	/**
	 * Removes the given listener from this undo manager.
	 * 
	 * @param listener the listener to be removed
	 */
	public void removeListener(IUndoManagerListener listener);
	
	/**
	 * The infrastructure is goind to perform the given change.
	 * 
	 * @param change the change to be performed.
	 */
	public void aboutToPerformChange(Change change);
	
	/**
	 * The infrastructure has performed the given change.
	 * 
	 * @param change the change that was performed
	 * @param undo the corresponding undo change or <code>null</code>
	 *  if no undo exists
	 * @param e <code>null</code> if the change got executed
	 *  successfully; otherwise the catched exception
	 */
	public void changePerformed(Change change, Change undo, Exception e);

	/**
	 * Adds a new undo change to this undo manager.
	 * 
	 * @param name the name of the refactoring the change was created
	 *  for. The name must not be <code>null</code>
	 * @param change the undo change. The change must not be <code>null</code>
	 */
	public void addUndo(String name, Change change);

	/**
	 * Returns <code>true</code> if there is anything to undo, otherwise
	 * <code>false</code>.
	 * 
	 * @return <code>true</code> if there is anything to undo, otherwise
	 *  <code>false</code>
	 */
	public boolean anythingToUndo();
	
	/**
	 * Returns the name of the top most undo.
	 * 
	 * @return the top most undo name. The main purpose of the name is to
	 * render it in the UI. Returns <code>null</code> if there aren't any changes to undo
	 */
	public String peekUndoName();
	
	/**
	 * Undo the top most undo change.
	 * 
	 * @param pm a progress monitor to report progress during performing
	 *  the undo change. The progress monitor must not be <code>null</code>
	 * @return a status indicating if the undo preflight produced any error
	 */	
	public RefactoringStatus performUndo(IProgressMonitor pm) throws CoreException;

	/**
	 * Returns <code>true</code> if there is anything to redo, otherwise
	 * <code>false</code>.
	 * 
	 * @return <code>true</code> if there is anything to redo, otherwise
	 *  <code>false</code>
	 */
	public boolean anythingToRedo();
	
	/**
	 * Returns the name of the top most redo.
	 * 
	 * @return the top most redo name. The main purpose of the name is to
	 * render it in the UI. Returns <code>null</code> if there are no any changes to redo.
	 */
	public String peekRedoName();
	
	/**
	 * Redo the top most redo change.
	 * 
	 * @param pm a progress monitor to report progress during performing
	 *  the redo change. The progress monitor must not be <code>null</code>
	 * @return a status indicating if the undo preflight produced any error
	 */	
	public RefactoringStatus performRedo(IProgressMonitor pm) throws CoreException;
	
	/**
	 * Flushes the undo manager's undo and redo stacks.
	 */	
	public void flush();
	
	/**
	 * Shut down the undo manager. 
	 */
	public void shutdown();
}
