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

import org.eclipse.core.resources.IFile;

import org.eclipse.jdt.core.BufferChangedEvent;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IBufferChangedListener;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.Corext;
import org.eclipse.jdt.internal.corext.refactoring.base.Change;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeAbortException;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;

/* package */ class UndoCompilationUnitChange extends Change {

	private static class UndoTempWorkingCopyChange extends Change {
		private String fName;
		private IChange fUndoChange;
		private UndoEdit fEdit;
		private ICompilationUnit fUnit;
		private IBuffer fBuffer;
		private boolean fChanged;
		private IBufferChangedListener fListener;
		
		UndoTempWorkingCopyChange(String name, ICompilationUnit unit, UndoEdit edit) {
			Assert.isNotNull(name);
			Assert.isNotNull(edit);
			fName= name;
			fEdit= edit;
			fUnit= unit;
			fListener= new IBufferChangedListener() {
				public void bufferChanged(BufferChangedEvent event) {
					fChanged= true;
					if (fListener != null) {
						fBuffer.removeBufferChangedListener(fListener);
						fListener= null;
					}
				}
			};			
			try {
				fBuffer= unit.getBuffer();
				fBuffer.addBufferChangedListener(fListener);
			} catch (JavaModelException e) {
				fListener= null;
				fChanged= true;
			}
		}
		public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
			if (fChanged)
				return RefactoringStatus.createFatalErrorStatus("Buffer has changed");
			return new RefactoringStatus();
		}
		public void perform(ChangeContext context, IProgressMonitor pm) throws ChangeAbortException, CoreException {
			IDocument document= new Document(fBuffer.getContents());
			UndoEdit undo;
			try {
				undo= fEdit.apply(document, TextEdit.CREATE_UNDO);
			} catch (BadLocationException e) {
				throw new CoreException(new Status(IStatus.ERROR, Corext.getPluginId(), IStatus.ERROR, e.getMessage(), e));
			}
			fUndoChange= new UndoTempWorkingCopyChange(getName(), fUnit, undo);
		}
		public String getName() {
			return fName;
		}
		public IChange getUndoChange() {
			return fUndoChange;
		}
		public Object getModifiedLanguageElement() {
			return fUnit;
		}
	}

	private String fName;
	private IChange fChange;
	private ICompilationUnit fUnit;
	
	public UndoCompilationUnitChange(String name, ICompilationUnit unit, UndoEdit edit) {
		Assert.isNotNull(name);
		Assert.isNotNull(edit);
		fName= name;
		IFile file= (IFile)unit.getResource();
		if (file != null) {
			fChange= new UndoTextFileChange(name, file, edit);
		} else {
			fChange= new UndoTempWorkingCopyChange(name, unit, edit);
		}
	}
		
	public String getName() {
		return fName;
	}
	
	public IChange getUndoChange() {
		return fChange.getUndoChange();
	}
	
	public Object getModifiedLanguageElement() {
		return fChange.getModifiedLanguageElement();
	}
	
	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
		if (!fUnit.exists())
			return RefactoringStatus.createFatalErrorStatus("Compilation Unit doesn't exist");
		return fChange.isValid(pm);
	}
	
	public void perform(ChangeContext context, IProgressMonitor pm) throws ChangeAbortException, CoreException {
		fChange.perform(context, pm);
	}
}
