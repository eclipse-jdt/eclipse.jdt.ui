/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.base;

/**
 * An adapter implementation for <code>IUndoManagerListener</code>.
 */
public class UndoManagerAdapter implements IUndoManagerListener {

	/* (non-Javadoc)
	 * Method declared in IUndoManagerListener
	 */
	public void undoStackChanged(IUndoManager manager) {
	}
	
	/* (non-Javadoc)
	 * Method declared in IUndoManagerListener
	 */
	public void redoStackChanged(IUndoManager manager) {
	}
}

