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
package org.eclipse.text.edits;

import java.util.Iterator;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

/**
 * A <code>TextEditProcessor</code> manages a set of <code>Edit</code>s and applies
 * them as a whole to an <code>IDocument</code>. Clients should use the method <code>
 * canPerformEdits</code> to validate if all added edits can be executed.
 */
public class TextEditProcessor {
	
	private IDocument fDocument;
	private MultiTextEdit fRoot;
	
	/**
	 * Constructs a new edit processor for the given
	 * document.
	 * 
	 * @param document the document to manipulate
	 */
	public TextEditProcessor(IDocument document) {
		Assert.isNotNull(document);
		fDocument= document;
		fRoot= new MultiTextEdit(0, fDocument.getLength()); 
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
	 * Returns the edit processor's root edit.
	 * 
	 * @return the processor's root edit
	 */
	public TextEdit getRoot() {
		return fRoot;
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
	public void add(TextEdit edit) throws MalformedTreeException {
		checkIntegrity(edit, fDocument);
		fRoot.add(edit);
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
		return checkBufferLength(fRoot, fDocument.getLength()) == null;
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
	public UndoMemento performEdits() throws BadLocationException {
		return execute();
	}
	
	
	protected boolean considerEdit(TextEdit edit) {
		return true;
	}
		
	/* proctected */ void checkIntegrity() throws MalformedTreeException {
		TextEdit failure= checkBufferLength(fRoot, fDocument.getLength());
		if (failure != null) {
			throw new MalformedTreeException(failure.getParent(), failure, "End position lies outside of document range");
		}
	}
	
	//---- Helper methods ------------------------------------------------------------------------
		
	private static TextEdit checkBufferLength(TextEdit root, int bufferLength) {
		if (root.getExclusiveEnd() > bufferLength)
			return root;
		for (Iterator iter= root.iterator(); iter.hasNext();) {
			TextEdit edit= (TextEdit)iter.next();
			TextEdit failure= null;
			if ((failure= checkBufferLength(edit, bufferLength)) != null)
				return failure;
		}
		return null;
	}
	
	private static void checkIntegrity(TextEdit root, IDocument document) {
		root.checkIntegrity();
		for (Iterator iter= root.iterator(); iter.hasNext();) {
			TextEdit edit= (TextEdit)iter.next();
			checkIntegrity(edit, document);
		}
	}
	
	private UndoMemento execute() throws BadLocationException {
		Updater.DoUpdater updater= null;
		try {
			updater= Updater.createDoUpdater();
			fDocument.addDocumentListener(updater);
			updater.push(new TextEdit[] { fRoot });
			updater.setIndex(0);
			execute(fRoot, updater);
			return updater.undo;
		} finally {
			if (updater != null)
				fDocument.removeDocumentListener(updater);
		}
	}
	
	private void execute(TextEdit edit, Updater.DoUpdater updater) throws BadLocationException {
		if (edit.hasChildren()) {
			TextEdit[] children= edit.getChildren();
			updater.push(children);
			for (int i= children.length - 1; i >= 0; i--) {
				updater.setIndex(i);
				execute(children[i], updater);
			}
			updater.pop();
		}
		if (considerEdit(edit)) {
			updater.setActiveEdit(edit);
			edit.perform(fDocument);
		}
	}
}
