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

import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.IDocument;

/**
 * An <code>EditProcessor</code> manages a set of <code>Edit</code>s and applies
 * them as a whole to a <code>IDocument</code>. Clients should use the method <code>
 * canPerformEdits</code> to validate if all added edits follow can be executed.
 */
public class EditProcessor {
	
	private IDocument fDocument;
	private MultiTextEdit fRoot;
	private UndoMemento fUndoMemento;
	
	/**
	 * Constructs a new edit processor for the given
	 * document.
	 * 
	 * @param document the document to manipulate
	 */
	public EditProcessor(IDocument document) {
		Assert.isNotNull(document);
		fDocument= document;
	}
	
	/**
	 * Returns the document to be manipulated.
	 * 
	 * @return the document
	 */
	public IDocument getDocument() {
		return fDocument;
	}
	
	/**
	 * Adds an <code>Edit</code> to this edit processor. Adding an edit
	 * to an edit processor transfers ownership of the edit to the 
	 * processor. So after an edit has been added to a processor the 
	 * creator of the edit <b>must</b> not continue modifing the edit.
	 * 
	 * @param edit the edit to add
	 * @exception EditException if the text edit can not be added
	 * 	to this edit processor.
	 */
	public void add(TextEdit edit) throws IllegalEditException {
		Assert.isTrue(fUndoMemento == null);
		executeConnect(edit, fDocument);
		if (fRoot == null) {
			fRoot= new MultiTextEdit(0, fDocument.getLength());
		}
		fRoot.add(edit);
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
	public void add(UndoMemento undo) throws IllegalEditException {
		Assert.isTrue(fRoot == null);
		fUndoMemento= undo;
	}
	
	/**
	 * Checks if the processor can execute all its edits.
	 * 
	 * @return <code>true</code> if the edits can be executed. Return  <code>false
	 * 	</code>otherwise. One major reason why edits cannot be executed are wrong 
	 * offset or length values of edits. Calling perform in this case will very
	 * likely end in a <code>BadLocationException</code>.
	 */
	public boolean canPerformEdits() {
		if (fRoot != null && !checkBufferLength(fRoot, fDocument.getLength())) {
			return false;
		} else if (fUndoMemento != null) {
			return fUndoMemento.canPerform(fDocument.getLength());
		} 
		return true;
	}
	
	/**
	 * Clears the text buffer editor.
	 */
	public void clear() {
		fRoot= null;
		fUndoMemento= null;
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
	public UndoMemento performEdits() throws PerformEditException {
		try {
			if (fRoot != null) {
				return executeDo();
			} else if (fUndoMemento != null) {
				return executeUndo();
			} else {
				return new UndoMemento();
			}
		} finally {
			clear();
		}
	}
	
	//---- Helper methods ------------------------------------------------------------------------
		
	private static boolean checkBufferLength(TextEdit root, int bufferLength) {
		TextRange range= root.getTextRange();
		if (range.getExclusiveEnd() > bufferLength)
			return false;
		for (Iterator iter= root.iterator(); iter.hasNext();) {
			TextEdit edit= (TextEdit)iter.next();
			if (!checkBufferLength(edit, bufferLength))
				return false;
		}
		return true;
	}
	
	private static void executeConnect(TextEdit root, IDocument document) {
		TextRange oldRange= root.getTextRange();
		root.connect(document);
		TextRange newRange= root.getTextRange();
		if (oldRange.getOffset() != newRange.getOffset() || oldRange.getLength() != newRange.getLength())
			throw new IllegalEditException(root.getParent(), root, "Text edit changed during connect method");
		for (Iterator iter= root.iterator(); iter.hasNext();) {
			TextEdit edit= (TextEdit)iter.next();
			executeConnect(edit, document);
		}
	}
	
	private UndoMemento executeDo() throws PerformEditException {
		Updater updater= null;
		try {
			updater= Updater.createDoUpdater();
			fDocument.addDocumentListener(updater);
			execute(fRoot, updater);
			List executed= updater.getProcessedEdits();
			for (int i= executed.size() - 1; i >= 0; i--) {
				((TextEdit)executed.get(i)).performed();
			}
			return updater.undo;
		} finally {
			if (updater != null)
				fDocument.removeDocumentListener(updater);
		}
	}
	
	private void execute(TextEdit edit, Updater updater) throws PerformEditException {
		TextEdit[] children= edit.getChildren();
		for (int i= children.length - 1; i >= 0; i--) {
			execute(children[i], updater);
		}
		if (considerEdit(edit)) {
			try {
				updater.setActiveNode(edit);
				edit.perform(fDocument);
			} finally {
				updater.setActiveNode(null);
			}
		}
	}
	
	protected boolean considerEdit(TextEdit edit) {
		return true;
	}
	
	private UndoMemento executeUndo() throws PerformEditException {
		Updater updater= null;
		try {
			updater= Updater.createUndoUpdater();
			fDocument.addDocumentListener(updater);
			fUndoMemento.execute(fDocument);
			fUndoMemento.executed();
			return updater.undo;
		} finally {
			if (updater != null)
				fDocument.removeDocumentListener(updater);
		}
	}
}
