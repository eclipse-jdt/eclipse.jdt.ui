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
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IFile;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;

import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;

public class TextFileChange extends TextChange  {
	
	private IFile fFile;
	private boolean fSave= true;
	
	private int fAquireCount;
	private ITextFileBuffer fBuffer;

	/**
	 * Creates a new <code>TextFileChange</code> for the given file.
	 * s
	 * @param name the change's name mainly used to render the change in the UI
	 * @param file the file this text change operates on
	 */
	public TextFileChange(String name, IFile file) {
		super(name);
		Assert.isNotNull(file);
		fFile= file;
	}
	
	/**
	 * Sets the save state. If set to <code>true</code> the change will save the
	 * content of the file back to disk.
	 * 
	 * @param save whether or not the changes should be saved to disk
	 */
	public void setSave(boolean save) {
		fSave= save;
	}
	
	/**
	 * Returns the <code>IFile</code> this change is working on.
	 * 
	 * @return the file this change is working on
	 */
	public IFile getFile() {
		return fFile;
	}

	/**
	 * {@inheritDoc}
	 */
	public Object getModifiedLanguageElement(){
		return fFile;
	}
	
	public RefactoringStatus isValid(IProgressMonitor pm) {
		if (fSave) {
			return Checks.validateModifiesFiles(new IFile[] {fFile});
		}
		return new RefactoringStatus();
	}

	/**
	 * {@inheritDoc}
	 */
	protected IDocument aquireDocument(IProgressMonitor pm) throws CoreException {
		if (fAquireCount > 0)
			return fBuffer.getDocument();
		
		ITextFileBufferManager manager= FileBuffers.getTextFileBufferManager();
		IPath path= fFile.getFullPath();
		manager.connect(path, pm);
		fAquireCount++;
		fBuffer= manager.getTextFileBuffer(path);
		return fBuffer.getDocument();
	}
	
	/**
	 * {@inheritDoc}
	 */
	protected void commit(IProgressMonitor pm) throws CoreException {
		if (fSave) {
			fBuffer.commit(pm, false);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	protected void releaseDocument(IDocument document, IProgressMonitor pm) throws CoreException {
		Assert.isTrue(fAquireCount > 0);
		if (fAquireCount == 1) {
			ITextFileBufferManager manager= FileBuffers.getTextFileBufferManager();
			manager.disconnect(fFile.getFullPath(), pm);
		}
		fAquireCount--;
 	}
	
	/**
	 * {@inheritDoc}
	 */
	protected IChange createUndoChange(UndoEdit edit) throws CoreException {
		return new UndoTextFileChange(getName(), fFile, edit);
	}
}

