/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.changes;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.UndoMemento;

/**
 * A text change that operates directly on a text buffer. Note that the ownership
 * of the text buffer is still at the client of this class. So after performing the change
 * the client is responsible to save the text buffer if needed.
 */
public class TextBufferChange extends TextChange {

	private TextBuffer fBuffer;

	private static class UndoTextBufferChange extends UndoTextChange {
		private TextBuffer fBuffer;
		public UndoTextBufferChange(String name, TextBuffer buffer, int changeKind, UndoMemento undo) {
			super(name, changeKind, undo);
			fBuffer= buffer;
		}
		public Object getModifiedLanguageElement(){
			return null;
		}
		protected IChange createReverseChange(UndoMemento undo, int changeKind) {
			return new UndoTextBufferChange(getName(), fBuffer, changeKind, undo);
		}
		protected TextBuffer acquireTextBuffer() throws CoreException {
			return fBuffer;
		}
		protected void releaseTextBuffer(TextBuffer textBuffer) {
			// do nothing. 
		}
		protected TextBuffer createTextBuffer() throws CoreException {
			return TextBuffer.create(fBuffer.getContent());
		}
	}

	/**
	 * Creates a new <code>TextBufferChange</code> for the given
	 * <code>ITextBuffer</code>.
	 * 
	 * @param name the change's name mainly used to render the change in the UI.
	 * @param textBuffer the text buffer this change is working on
	 */
	public TextBufferChange(String name, TextBuffer textBuffer) {
		super(name);
		fBuffer= textBuffer;
		Assert.isNotNull(fBuffer);
	}
	
	/* non Java-doc
	 * @see IChange.getModifiedLanguageElement
	 */
	public Object getModifiedLanguageElement(){
		return null;
	}
	
	/* non java-doc
	 * Method declared in TextChange
	 */
	protected IChange createReverseChange(UndoMemento undo, int changeKind) {
		return new UndoTextBufferChange(getName(), fBuffer, changeKind, undo);
	}
	
	/* non java-doc
	 * Method declared in TextChange
	 */
	protected TextBuffer acquireTextBuffer() throws CoreException {
		return fBuffer;
	}
	
	/* non java-doc
	 * Method declared in TextChange
	 */
	protected void releaseTextBuffer(TextBuffer textBuffer) {
		// do nothing. 
	}

	/* non java-doc
	 * Method declared in TextChange
	 */
	protected TextBuffer createTextBuffer() throws CoreException {
		return TextBuffer.create(fBuffer.getContent());
	}
}

