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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IFile;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.internal.corext.Corext;
import org.eclipse.jdt.internal.corext.refactoring.base.Change;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeAbortException;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;

/**
 * TODO
 * @since 3.0
 */
public class UndoTextFileChange extends Change {
	
	private String fName;
	private UndoEdit fUndo;
	private IChange fUndoChange;
	private IFile fFile;
	private long fModificationStamp;
	
	public UndoTextFileChange(String name, IFile file, UndoEdit undo) {
		fName= name;
		fFile= file;
		fUndo= undo;
		fModificationStamp= fFile.getModificationStamp();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getName() {
		return fName;
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
		return fFile;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
		if (!fFile.exists())
			return RefactoringStatus.createFatalErrorStatus("File doesn't exist");
		if (fModificationStamp != fFile.getModificationStamp())
			return RefactoringStatus.createFatalErrorStatus("File changed");
		ITextFileBufferManager manager= FileBuffers.getTextFileBufferManager();
		// Don't connect. We want to check if the file is under modification right now
		ITextFileBuffer buffer= manager.getTextFileBuffer(fFile.getFullPath());
		if (buffer != null && buffer.isDirty()) {
			return RefactoringStatus.createFatalErrorStatus("Buffer is dirty");
		}
		return new RefactoringStatus();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void perform(ChangeContext context, IProgressMonitor pm) throws JavaModelException, ChangeAbortException {
		ITextFileBufferManager manager= FileBuffers.getTextFileBufferManager();
		pm.beginTask("", 2); //$NON-NLS-1$
		try {
			ITextFileBuffer buffer= null;
			try {
				manager.connect(fFile.getFullPath(), new SubProgressMonitor(pm, 1));
				buffer= manager.getTextFileBuffer(fFile.getFullPath());
				IDocument document= buffer.getDocument();
				UndoEdit redo= fUndo.apply(document, TextEdit.CREATE_UNDO);
				buffer.commit(pm, false);
				fUndoChange= new UndoTextFileChange(getName(), fFile, redo);
			} catch (BadLocationException e) {
				throw new CoreException(createStatus(e));
			} finally {
				if (buffer != null)
					manager.disconnect(fFile.getFullPath(), new SubProgressMonitor(pm, 1));
			}
		} catch (CoreException e) {
			throw new JavaModelException(e);
		}
	}
	
	private static IStatus createStatus(BadLocationException e) {
		return new Status(IStatus.ERROR, Corext.getPluginId(), IStatus.ERROR, e.getMessage(), e);
	}
}