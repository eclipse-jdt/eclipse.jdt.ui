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

import org.eclipse.text.edits.UndoEdit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;

/**
 * TODO
 * @since 3.0
 */
public class DocumentChange extends TextChange {

	private IDocument fDocument;
	
	/**
	 * Creates a new <code>DocumentChange</code> for the given 
	 * {@link IDocument}.
	 * 
	 * @param name the change's name. Has to be a human readable name.
	 * @param document the document this change is working on
	 */
	public DocumentChange(String name, IDocument document) {
		super(name);
		Assert.isNotNull(document);
		fDocument= document;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Object getModifiedLanguageElement(){
		return fDocument;
	}

	/**
	 * {@inheritDoc}
	 */
	protected IDocument aquireDocument(IProgressMonitor pm) throws CoreException {
		return fDocument;
	}
	
	/**
	 * {@inheritDoc}
	 */
	protected void commit(IProgressMonitor pm) throws CoreException {
		// do nothing
	}
	
	/**
	 * {@inheritDoc}
	 */
	protected void releaseDocument(IDocument document, IProgressMonitor pm) throws CoreException {
		//do nothing
	}
	
	/**
	 * {@inheritDoc}
	 */
	protected IChange createUndoChange(UndoEdit edit) throws CoreException {
		return new UndoDocumentChange(getName(), fDocument, edit);
	}	
}

