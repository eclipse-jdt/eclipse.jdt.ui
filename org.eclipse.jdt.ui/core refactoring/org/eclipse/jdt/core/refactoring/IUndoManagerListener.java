/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */
package org.eclipse.jdt.core.refactoring;

/**
 * Listener to monitor changes made to an <code>UndoManager</code>
 */
public interface IUndoManagerListener {
	
	/**
	 * This method is called by the undo manager if an undo change has been 
	 * added to it.
	 */
	public void undoAdded();
	
	/**
	 * This method is called by the undo manager if a redo change has been 
	 * added to it.
	 */
	public void redoAdded();
	
	/**
	 * This method is called by the undo manager if the undo manager's undo
	 * stack got empty.
	 */
	public void noMoreUndos();

	/**
	 * This method is called by the undo manager if the undo manager's redo
	 * stack got empty.
	 */
	public void noMoreRedos();
}
