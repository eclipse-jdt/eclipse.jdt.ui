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
package org.eclipse.jdt.internal.corext.refactoring.changes;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.NullChange;
import org.eclipse.jdt.internal.corext.refactoring.base.Change;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeAbortException;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBufferEditor;
import org.eclipse.jdt.internal.corext.textmanipulation.UndoMemento;

public abstract class AbstractTextChange extends Change {

	private String fName;
	private int fChangeKind;
	private IChange fUndoChange;

	protected final static int ORIGINAL_CHANGE=		0;
	protected final static int UNDO_CHANGE=				1;
	protected final static int REDO_CHANGE=				2;

	/**
	 * Creates a new <code>TextChange</code> with the given name.
	 * 
	 * @param name the change's name mainly used to render the change in the UI.
	 * @param changeKind a flag indicating if the change is a <code>ORIGINAL_CHANGE</code>,
	 * 	a <code>UNDO_CHANGE</code> or a <code>REDO_CHANGE</code>
	 */
	protected AbstractTextChange(String name, int changeKind) {
		fName= name;
		Assert.isNotNull(fName);
		fChangeKind= changeKind;
		Assert.isTrue(0 <= fChangeKind && fChangeKind <= 2);
	}
	
	/**
	 * Acquires a new text buffer to perform the changes managed by this
	 * text buffer change. Two subsequent calls to this method must
	 * return the identical <code>ITextBuffer</code> object.
	 * 
	 * @return the acquired text buffer
	 */
	protected abstract TextBuffer acquireTextBuffer() throws CoreException; 
	
	/**
	 * Releases the given text buffer. The given text buffer is not usable
	 * anymore after calling this method.
	 * 
	 * @param textBuffer the text buffer to be released
	 */
	protected abstract void releaseTextBuffer(TextBuffer textBuffer);

	/**
	 * Create a new <code>TextBuffer</code>. Any call to this method
	 * must create a new <code>TextBuffer</code>instance.
	 * 
	 * @return the created text buffer
	 */	
	protected abstract TextBuffer createTextBuffer() throws CoreException;
	
	/**
	 * Adds the <code>TextEdits</code> managed by this change to the given
	 * text buffer editor.
	 * 
	 * @param editor the text buffer edit
	 * @param copy if <code>true</code> the edits are copied before adding.
	 * 	Otherwise the original edits are added.
	 */
	protected abstract void addTextEdits(TextBufferEditor editor) throws CoreException;
	
	/**
	 * Creates a <code>IChange</code> that can undo this change.
	 * 
	 * @param edits the text edits that can undo the edits performed by this change
	 * @param changeKind the change kind of the reverse change. Either <code>
	 * 	UNDO_CHANGE</code> or </code>REDO_CHANGE</code>
	 * @return a change that can undo this change
	 */
	protected abstract IChange createReverseChange(UndoMemento edits , int changeKind);
	
	/**
	 * Returns <code>true</code> if this change is a reverse change. This is the case for an undo
	 * or a redo change. Returns <code>false</code> if the change is an original 
	 * change.
	 * 
	 * @return whether or not this change is a reverse change
	 */
	public boolean isReverseChange() {
		return fChangeKind != ORIGINAL_CHANGE;
	}
	
	protected int getReverseKind() {
		if (fChangeKind == ORIGINAL_CHANGE || fChangeKind == REDO_CHANGE)
			return UNDO_CHANGE;
		else
			return REDO_CHANGE;
	}	
	
	/* (Non-Javadoc)
	 * Method declared in IChange.
	 */
	public String getName(){
		return fName;
	}
	
	/* Non-Javadoc
	 * Method declared in IChange
	 */
	public IChange getUndoChange() {
		return fUndoChange;
	}
	
	/* Non-Javadoc
	 * Method declared in IChange
	 */
	public void perform(ChangeContext context, IProgressMonitor pm) throws JavaModelException, ChangeAbortException {
		if (!isActive()) {
			fUndoChange= new NullChange();
			return;
		}
		TextBufferEditor editor= null;
		try {
			fUndoChange= null;
			editor= new TextBufferEditor(acquireTextBuffer());
			addTextEdits(editor);
			fUndoChange= createReverseChange(editor.performEdits(pm), getReverseKind());
		} catch (Exception e) {
			handleException(context, e);
		} finally {
			if (editor != null) {
				editor.clear();
				releaseTextBuffer(editor.getTextBuffer());
			}
		}
	}		
}

