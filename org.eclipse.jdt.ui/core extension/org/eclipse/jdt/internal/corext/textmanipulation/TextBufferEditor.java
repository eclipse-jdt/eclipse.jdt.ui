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
package org.eclipse.jdt.internal.corext.textmanipulation;

import org.eclipse.text.edits.TextEditProcessor;
import org.eclipse.text.edits.UndoMemento;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jface.text.BadLocationException;

import org.eclipse.jdt.internal.corext.Assert;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class TextBufferEditor extends TextEditProcessor {

	private TextBuffer fBuffer;
	private UndoMemento fUndoMemento;
		
	/**
	 * Creates a new <code>TextBufferEditor</code> for the given 
	 * <code>TextBuffer</code>.
	 * 
	 * @param the text buffer this editor is working on.
	 */
	public TextBufferEditor(TextBuffer buffer) {
		super(buffer.getDocument());
		fBuffer= buffer;
	}
	
	/**
	 * Returns the text buffer this editor is working on.
	 * 
	 * @return the text buffer this editor is working on
	 */
	public TextBuffer getTextBuffer() {
		return fBuffer;
	}
	
	/**
	 * Adds an undo memento to this edit processor. Adding an undo memento
	 * transfers ownership of the memento to the processor. So after a memento 
	 * has been added the creator of that memento <b>must</b> not continue
	 * modifying it.
	 * 
	 * @param undo the undo memento to add
	 * @exception EditException if the undo memento can not be added
	 * 	to this processor
	 */
	public void add(UndoMemento undo) {
		Assert.isTrue(!getRoot().hasChildren());
		fUndoMemento= undo;
	}
	
	public boolean canPerformEdits() {
		if (fUndoMemento != null)
			return fUndoMemento.canApply(getDocument());
		else 
			return super.canPerformEdits();
	}
	
	/**
	 * Executes the text edits added to this text buffer editor and clears all added
	 * text edits.
	 * 
	 * @param pm a progress monitor to report progress or <code>null</code> if
	 * 	no progress is desired.
	 * @return an object representing the undo of the executed <code>TextEdit</code>s
	 * @exception CoreException if the edits cannot be executed
	 */
	public UndoMemento performEdits(IProgressMonitor pm) throws CoreException {
		try {
			if (fUndoMemento != null) {
				return fUndoMemento.apply(getDocument());
			} else {
				return super.performEdits();
			}
		} catch (BadLocationException e) {
			throw new CoreException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(),
				IStatus.ERROR, e.getMessage(), e));
		}
	}	
}

