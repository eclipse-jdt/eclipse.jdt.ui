/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.changes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.core.codemanipulation.TextBuffer;
import org.eclipse.jdt.internal.core.codemanipulation.TextRegion;
import org.eclipse.jdt.internal.core.codemanipulation.TextEdit;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.core.refactoring.base.Change;
import org.eclipse.jdt.internal.core.refactoring.base.ChangeAbortException;
import org.eclipse.jdt.internal.core.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;
import org.eclipse.jdt.internal.core.refactoring.base.ITextChange;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.core.codemanipulation.*;

public abstract class TextChange extends AbstractTextChange {

	public static class TextEditChange {
		private String fName;
		private boolean fIsActive;
		private TextEdit fTextEdit;
		private TextChange fTextChange;
		
		private TextEditChange(String name, TextEdit edit, TextChange change) {
			fName= name;
			fTextEdit= edit;
			fTextChange= change;
			fIsActive= true;
		}
		public String getName() {
			return fName;
		}
		public void setActive(boolean active) {
			fIsActive= active;
		}
		public boolean isActive() {
			return fIsActive;
		}
		public TextEdit getTextEdit() {
			return fTextEdit;
		}
		public TextChange getTextChange() {
			return fTextChange;
		}
	}

	private List fTextEditChanges;

	/**
	 * Creates a new <code>TextChange</code> with the given name.
	 * 
	 * @param name the change's name mainly used to render the change in the UI.
	 */
	protected TextChange(String name) {
		super(name, ORIGINAL_CHANGE);
		fTextEditChanges= new ArrayList(5);
	}
	
	/**
	 * Adds a text edit object to this text buffer change.
	 *
	 * @param name the name of the given text edit. The name is used to render this 
	 * 	change in the UI.
	 * @param edit the text edit to add
	 */
	public void addTextEdit(String name, TextEdit edit) {
		Assert.isNotNull(name);
		Assert.isNotNull(edit);
		fTextEditChanges.add(new TextEditChange(name, edit, this));
	}
	
	/**
	 * Returns the text edit changes managed by this text change.
	 * 
	 * @return the text edit changes
	 * @see isReverseChange
	 */
	public TextEditChange[] getTextEditChanges() {
		return (TextEditChange[])fTextEditChanges.toArray(new TextEditChange[fTextEditChanges.size()]);
	}
	
	/**
	 * Returns the text this change is working on.
	 * 
	 * @return the original text.
	 * @exception JavaModelException if text cannot be accessed
	 */
	public String getCurrentContent() throws JavaModelException {
		TextBuffer buffer= null;
		try {
			buffer= acquireTextBuffer();
			return buffer.getContent();
		} catch (CoreException e) {
			throw new JavaModelException(e);
		} finally {
			if (buffer != null)
				releaseTextBuffer(buffer);
		}
	}
	
	/**
	 * Returns a preview of the change without actually modifying the underlying text.
	 * 
	 * @return the change's preview
	 * @exception JavaModelException if the preview could not be created
	 */
	public String getPreviewContent() throws JavaModelException {
		try {
			TextBufferEditor editor= new TextBufferEditor(createTextBuffer());
			addTextEdits(editor, true);
			editor.performEdits(new NullProgressMonitor());
			return editor.getTextBuffer().getContent();
		} catch (CoreException e) {
			throw new JavaModelException(e);
		}
	}
	
	/**
	 * Returns the current content to be modified by the given text edit change. The
	 * text edit change must have been added to this <code>TextChange<code>
	 * by calling <code>addTextEdit()</code>
	 * 
	 * @param change the text edit change for which the content is requested
	 * @param surroundingLines the number of surrounding lines added at top and bottom
	 * 	of the content
	 * @return the current content to be modified by the given text edit change
	 */
	public String getCurrentContent(TextEditChange change, int surroundingLines) throws CoreException {
		return getContent(change, surroundingLines, false);
	}
	
	/**
	 * Returns the preview of the given text edit change. The text edit change must 
	 * have been added to this <code>TextChange<code> by calling <code>addTextEdit()
	 * </code>
	 * 
	 * @param change the text edit change for which the preview is requested
	 * @param surroundingLines the number of surrounding lines added at top and bottom
	 * 	of the preview
	 * @return the preview of the given text edit change
	 */
	public String getPreviewContent(TextEditChange change, int surroundingLines) throws CoreException {
		return getContent(change, surroundingLines, true);
	}

	/* (Non-Javadoc)
	 * Method declared in IChange.
	 */
	public void setActive(boolean active) {
		super.setActive(active);
		for (Iterator iter= fTextEditChanges.iterator(); iter.hasNext();) {
			TextEditChange element= (TextEditChange) iter.next();
			element.setActive(active);
		}
	}
	
	/* non Java-doc
	 * @see AbstractTextChange#addTextEdits
	 */
	protected void addTextEdits(TextBufferEditor editor, boolean copy) throws CoreException {
		for (Iterator iter= fTextEditChanges.iterator(); iter.hasNext(); ) {
			TextEditChange edit= (TextEditChange)iter.next();
			if (edit.isActive()) {
				TextEdit textEdit= edit.getTextEdit();
				editor.addTextEdit(copy ? textEdit.copy() : textEdit);
			}
		}		
	}
	
	private String getContent(TextEditChange change, int surroundingLines, boolean preview) throws CoreException {
		Assert.isTrue(change.getTextChange() == this);
		TextBuffer buffer= createTextBuffer();
		TextEdit edit= change.getTextEdit();
		if (preview) {
			TextBufferEditor editor= new TextBufferEditor(buffer);
			editor.addTextEdit(edit.copy());
			editor.performEdits(new NullProgressMonitor());
		}
		int[] offsetAndEnd= getOffsetAndEnd(edit);
		int startLine= Math.max(buffer.getLineOfOffset(offsetAndEnd[0]) - surroundingLines, 0);
		int endLine= Math.min(
			buffer.getLineOfOffset(offsetAndEnd[1]) + surroundingLines,
			buffer.getNumberOfLines());
		int offset= buffer.getLineInformation(startLine).getOffset();
		TextRegion region= buffer.getLineInformation(endLine);
		int length = region.getOffset() + region.getLength() - offset;
		return buffer.getContent(offset, length);
	}
	
	private static int[] getOffsetAndEnd(TextEdit edit) {
		TextPosition[] positions= edit.getTextPositions();
		if (positions == null || positions.length == 0)
			return new int[] {0,0};
		int offset= 0;
		int end= 0;
		for (int i= 0; i < positions.length; i++) {
			TextPosition position= positions[i];
			offset= Math.max(offset, position.getOffset());
			end= Math.max(end, position.getOffset() + position.getLength());
		}
		return new int[] {offset, end };
	}
}

