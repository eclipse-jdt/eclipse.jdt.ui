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

import org.eclipse.text.edits.UndoEdit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.internal.corext.refactoring.base.Change;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;

/**
 * A text change that operates directly on a text buffer. Note that the ownership
 * of the text buffer is still at the client of this class. So after performing the change
 * the client is responsible to save the text buffer if needed.
 */
public class TextBufferChange extends TextChange {

	private TextBuffer fBuffer;
	private DocumentChange fChange;
	
	/**
	 * Creates a new <code>TextBufferChange</code> for the given
	 * <code>ITextBuffer</code>.
	 * 
	 * @param name the change's name mainly used to render the change in the UI.
	 * @param textBuffer the text buffer this change is working on
	 */
	public TextBufferChange(String name, TextBuffer textBuffer) {
		super(name);
		fChange= new DocumentChange(name, textBuffer.getDocument());
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Object getModifiedElement(){
		return fBuffer;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void initializeValidationData(IProgressMonitor pm) throws CoreException {
		fChange.initializeValidationData(pm);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
		return fChange.isValid(pm);
	}

	/**
	 * {@inheritDoc}
	 */
	protected IDocument aquireDocument(IProgressMonitor pm) throws CoreException {
		return fChange.aquireDocument(pm);
	}

	/**
	 * {@inheritDoc}
	 */
	protected void commit(IDocument document, IProgressMonitor pm) throws CoreException {
		fChange.commit(document, pm);
	}
	
	/**
	 * {@inheritDoc}
	 */
	protected void releaseDocument(IDocument document, IProgressMonitor pm) throws CoreException {
		fChange.releaseDocument(document, pm);
	}

	/**
	 * {@inheritDoc}
	 */
	protected Change createUndoChange(UndoEdit edit) throws CoreException {
		return fChange.createUndoChange(edit);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		fChange.setEnabled(enabled);
	}
}

