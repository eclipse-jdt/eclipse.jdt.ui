/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.base;

/**
 * An adapter implementation for <code>IUndoManagerListener</code>.
 */
public class UndoManagerAdapter implements IUndoManagerListener {

	/* (non-Javadoc)
	 * Method declared in IUndoManagerListener
	 */
	public void undoAdded() {
	}
	
	/* (non-Javadoc)
	 * Method declared in IUndoManagerListener
	 */
	public void redoAdded() {
	}
	
	/* (non-Javadoc)
	 * Method declared in IUndoManagerListener
	 */
	public void noMoreUndos() {
	}
	 
	/* (non-Javadoc)
	 * Method declared in IUndoManagerListener
	 */
	public void noMoreRedos() {
	}
}

