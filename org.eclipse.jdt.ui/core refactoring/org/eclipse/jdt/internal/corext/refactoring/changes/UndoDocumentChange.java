/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.changes;

import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.UndoEdit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;

import org.eclipse.jdt.internal.corext.refactoring.base.Change;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeAbortException;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;

/**
 * TODO
 * @since 3.0
 */
public class UndoDocumentChange extends Change {
	
	private UndoEdit fUndo;
	private IChange fUndoChange;
	private IDocument fDocument;
	private boolean fChanged;
	private IDocumentListener fListner= new IDocumentListener() {
		public void documentAboutToBeChanged(DocumentEvent event) {
		}
		public void documentChanged(DocumentEvent event) {
			fChanged= true;
			if (fListner != null) {
				event.getDocument().removeDocumentListener(fListner);
				fListner= null;
			}
		}
	};
	
	public UndoDocumentChange(String name, IDocument document, UndoEdit undo) {
		super(name);
		fUndo= undo;
		fDocument= document;
		document.addDocumentListener(fListner);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public IChange getUndoChange() {
		return fUndoChange;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Object getModifiedLanguageElement() {
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
		if (fChanged)
			return RefactoringStatus.createFatalErrorStatus("Buffer has changed");
		return new RefactoringStatus();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void perform(ChangeContext context, IProgressMonitor pm) throws JavaModelException, ChangeAbortException {
		try {
			try {
				UndoEdit redo= fUndo.apply(fDocument, TextEdit.CREATE_UNDO);
				fUndoChange= new UndoDocumentChange(getName(), fDocument, redo);
				if (fListner != null) {
					fDocument.removeDocumentListener(fListner);
					fListner= null;
				}
			} catch (BadLocationException e) {
				throw Changes.asCoreException(e);
			}
		} catch (CoreException e) {
			throw new JavaModelException(e);
		}
	}
}