/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.codemanipulation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.internal.utils.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.JavaModelException;

/**
 * A <code>TextBufferEditor</code> manages a set of <code>TextEdit</code>s and applies
 * them as a whole to a <code>TextBuffer</code>. Added <code>TextEdit</code>s must 
 * not overlap. The only exception from this rule are insertion point. There can be more than
 * one insert point at the same text position. Clients should use the method <code>
 * canPerformEdits</code> to validate if all added text edits follow these rules.
 * <p>
 * Clients can attach more than one <code>TextBufferEditor</code> to a single <code>
 * TextBuffer</code>. If so <code>canPerformEdits</code> validates all text edits from
 * all text buffer editors working on the same text buffer.
 */
public class TextBufferEditor {
	
	private static class UndoTextEdits implements IUndoTextEdits {
		private List fUndos;
		public UndoTextEdits(int size) {
			fUndos= new ArrayList(size);
		}
		public void add(TextEdit undo) {
			fUndos.add(undo);
		}
		public void addTo(TextBufferEditor manipulator) throws CoreException {
			for (int i= fUndos.size() - 1; i >= 0; i--) {
				manipulator.addTextEdit((TextEdit)fUndos.get(i));
			}
		}
	}
	
	private TextBuffer fBuffer;
	private List fEdits;

	/**
	 * Creates a new <code>TextBufferEditor</code> for the given 
	 * <code>TextBuffer</code>.
	 * 
	 * @param the text buffer this editor is working on.
	 */
	public TextBufferEditor(TextBuffer buffer) {
		fBuffer= buffer;
		Assert.isNotNull(fBuffer);
		fEdits= new ArrayList(5);
	}
	
	/**
	 * Returns the text buffer this editor is working on.
	 * 
	 * @return the text buffer this editor is working on
	 */
	public TextBuffer getTextBuffer() {
		return fBuffer;
	}
	
	/**
	 * Adds a <code>TextEdit</code> to this text editor. All added text edits are
	 * executed by calling <code>performEdits</code>.
	 * 
	 * @param edit the text edit to be added
	 * @exception CoreException if the <code>TextEdit</code> can not be added
	 * 	to this text buffer editor
	 */	
	public void addTextEdit(TextEdit edit) throws CoreException {
		Assert.isNotNull(edit);
		if (fBuffer == null)
			throw new JavaModelException(null, IJavaModelStatusConstants.ELEMENT_DOES_NOT_EXIST);
		fEdits.add(edit);
		edit.connect(fBuffer);
		TextPosition[] positions= edit.getTextPositions();
		for (int i= 0; i < positions.length; i++) {
			fBuffer.addPosition(positions[i]);
		}
	}
	
	/**
	 * Checks if the <code>TextEdit</code> added to this text editor can be executed.
	 * 
	 * @return <code>true</code> if the edits can be executed. Return  <code>false
	 * 	</code>otherwise. One major reason why text edits cannot be executed
	 * 	is a wrong offset or length value of a <code>TextEdit</code>.
	 */
	public boolean canPerformEdits() {
		if (fBuffer == null)
			return false;
		return fBuffer.validatePositions();
	}
	
	/**
	 * Clears the text buffer editor.
	 */
	public void clear() {
		for (Iterator iter= fEdits.iterator(); iter.hasNext();) {
			TextEdit edit= (TextEdit) iter.next();
			TextPosition[] positions= edit.getTextPositions();
			for (int i= 0; i < positions.length; i++) {
				fBuffer.removePosition(positions[i]);
			}
		}
		fEdits.clear();
	}
	
	/**
	 * Executes the text edits added to this text buffer editor and clears all added
	 * text edits.
	 * 
	 * @param pm a progress monitor to report progress
	 * @return an object representing the undo of the executed <code>TextEdit</code>s
	 * @exception JavaModelException if the edits cannot be executed.
	 */
	public IUndoTextEdits performEdits(IProgressMonitor pm) throws CoreException {
		int size= fEdits.size();
		if (size == 0)
			return new UndoTextEdits(0);
		if (fBuffer == null)
			throw new JavaModelException(null, IJavaModelStatusConstants.ELEMENT_DOES_NOT_EXIST);

		try {
			UndoTextEdits undo= new UndoTextEdits(size);
			pm.beginTask("", size + 10);
			for (Iterator iter= fEdits.iterator(); iter.hasNext();) {
				TextEdit edit= (TextEdit) iter.next();
				fBuffer.setCurrentPositions(edit.getTextPositions());
				undo.add(edit.perform(fBuffer));
				pm.worked(1);
			}
			for (Iterator iter= fEdits.iterator(); iter.hasNext();) {
				TextEdit edit= (TextEdit) iter.next();
				edit.performed();
				TextPosition[] positions= edit.getTextPositions();
				for (int i= 0; i < positions.length; i++) {
					fBuffer.removePosition(positions[i]);
				}
			}
			pm.worked(10);
			return undo;
		} finally {
			pm.done();
			fEdits.clear();
		}
	}
}

