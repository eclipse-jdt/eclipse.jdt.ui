/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.changes;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.core.codemanipulation.IUndoTextEdits;
import org.eclipse.jdt.internal.core.codemanipulation.TextBuffer;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.base.ChangeAbortException;
import org.eclipse.jdt.internal.core.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;

public class TextFileChange extends TextChange  {

	protected static class UndoTextFileChange extends UndoTextChange {
		private IFile fFile;
		private TextBuffer fAcquiredTextBuffer;
		private int fAcquireCounter;
		public UndoTextFileChange(String name, IFile file, int changeKind, IUndoTextEdits undos) {
			super(name, changeKind, undos);
			fFile= file;
		}
		public Object getModifiedLanguageElement(){
			return fFile;
		}
		protected TextBuffer acquireTextBuffer() throws CoreException {
			fAcquiredTextBuffer= TextBuffer.acquire(fFile);
			fAcquireCounter++;
			return fAcquiredTextBuffer;
		}
		protected void releaseTextBuffer(TextBuffer textBuffer) {
			TextBuffer.release(textBuffer);
			if (--fAcquireCounter == 0)
				fAcquiredTextBuffer= null;
		}
		protected TextBuffer createTextBuffer() throws CoreException {
			return TextBuffer.create(fFile);
		}
		protected IChange createReverseChange(IUndoTextEdits edits, int changeKind) {
			return new UndoTextFileChange(getName(), fFile, changeKind, edits);
		}
		public void perform(ChangeContext context, IProgressMonitor pm) throws JavaModelException, ChangeAbortException {
			try{
				pm.beginTask("", 10);
				super.perform(context, new SubProgressMonitor(pm, 8));
				TextBuffer.aboutToChange(fAcquiredTextBuffer);
				TextBuffer.save(fAcquiredTextBuffer, new SubProgressMonitor(pm, 2));
			} catch (Exception e) {
				handleException(context, e);
			} finally {
				pm.done();
			}
		}
		public void performed() {
			try {
				TextBuffer.changed(fAcquiredTextBuffer);
			} catch (CoreException e) {
				Assert.isTrue(false, "Should not happen since the buffer is acquired through a text buffer manager");	
			}
			super.performed();
		}		
	}

	private IFile fFile;
	private TextBuffer fAcquiredTextBuffer;
	private int fAcquireCounter;

	/**
	 * Creates a new <code>TextFileChange</code> for the given file.
	 * 
	 * @param name the change's name mainly used to render the change in the UI
	 * @param file the file this text change operates on
	 */
	public TextFileChange(String name, IFile file) {
		super(name);
		fFile= file;
		Assert.isNotNull(fFile);
	}
		
	/* non java-doc
	 * Method declared in TextChange
	 */
	protected TextBuffer acquireTextBuffer() throws CoreException {
		fAcquiredTextBuffer= TextBuffer.acquire(fFile);
		fAcquireCounter++;
		return fAcquiredTextBuffer;
	}
	
	/* non java-doc
	 * Method declared in TextChange
	 */
	protected void releaseTextBuffer(TextBuffer textBuffer) {
		TextBuffer.release(textBuffer);
		if (--fAcquireCounter == 0)
			fAcquiredTextBuffer= null;
	}

	/* non java-doc
	 * Method declared in TextChange
	 */
	protected TextBuffer createTextBuffer() throws CoreException {
		return TextBuffer.create(fFile);
	}
	
	/* non java-doc
	 * Method declared in TextChange
	 */
	protected IChange createReverseChange(IUndoTextEdits undos, int changeKind) {
		return new UndoTextFileChange(getName(), fFile, changeKind, undos);
	}
	
	/* non java-doc
	 * Method declared in IChange.
	 */
	public Object getModifiedLanguageElement(){
		return fFile;
	}
	
	/* non java-doc
	 * Method declared in TextChange
	 */
	public void perform(ChangeContext context, IProgressMonitor pm) throws JavaModelException, ChangeAbortException {
		try{
			pm.beginTask("", 10);
			super.perform(context, new SubProgressMonitor(pm, 8));
			TextBuffer.aboutToChange(fAcquiredTextBuffer);
			TextBuffer.save(fAcquiredTextBuffer, new SubProgressMonitor(pm, 2));
		} catch (Exception e) {
			handleException(context, e);
		} finally {
			pm.done();
		}
	}
	
	/* non java-doc
	 * Method declared in TextChange
	 */
	public void performed() {
		try {
			TextBuffer.changed(fAcquiredTextBuffer);
		} catch (CoreException e) {
			Assert.isTrue(false, "Should not happen since the buffer is acquired through a text buffer manager");	
		}
		super.performed();
	}		
}

