/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.changes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.textmanipulation.MultiTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBufferEditor;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRegion;
import org.eclipse.jdt.internal.corext.refactoring.Assert;

public abstract class TextChange extends AbstractTextChange {

	public static abstract class EditChange {
		private String fName;
		private boolean fIsActive;
		private TextChange fTextChange;
		
		/* package */ EditChange(String name, TextChange change) {
			fName= name;
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
		public TextChange getTextChange() {
			return fTextChange;
		}
		public abstract TextRange getTextRange();
		public abstract Object getModifiedElement();

		protected abstract void addTo(TextBufferEditor editor, boolean copy, Map positionMap) throws CoreException;
		abstract Object getEdit();
	}

	private static class TextEditChange extends EditChange {
		private TextEdit fEdit;
		private TextEditChange(String name, TextEdit edit, TextChange change) {
			super(name, change);
			fEdit= edit;
		}
		protected void addTo(TextBufferEditor editor, boolean copy, Map positionMap) throws CoreException {
			TextEdit edit= copy ? fEdit.copy() : fEdit;
			if (positionMap != null)
				positionMap.put(fEdit, edit);
			editor.add(edit);
		}
		public TextRange getTextRange() {
			return fEdit.getTextRange();
		}
		public Object getModifiedElement() {
			return fEdit.getModifiedElement();
		}
		Object getEdit() {
			return fEdit;
		}
	}

	private static class MultiTextEditChange extends EditChange {
		private MultiTextEdit fEdit;
		private MultiTextEditChange(String name, MultiTextEdit edit, TextChange change) {
			super(name, change);
			fEdit= edit;
		}
		protected void addTo(TextBufferEditor editor, boolean copy, Map positionMap) throws CoreException {
			MultiTextEdit edit= copy ? fEdit.copy() : fEdit;
			if (positionMap != null)
				positionMap.put(fEdit, edit);
			editor.add(edit);
		}
		public TextRange getTextRange() {
			return fEdit.getTextRange();
		}
		public Object getModifiedElement() {
			return fEdit.getModifiedElement();
		}
		Object getEdit() {
			return fEdit;
		}
	}

	private List fTextEditChanges;
	private Map fPositionMap;

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
	 * Addes a multi text edit to this text buffer change.
	 * 
	 * @param name the name of the given multi text edit. The name is used to render this
	 * 	change in the UI
	 * @param edit the multi text edit to add
	 */
	public void addTextEdit(String name, MultiTextEdit edit) {
		Assert.isNotNull(name);
		Assert.isNotNull(edit);
		fTextEditChanges.add(new MultiTextEditChange(name, edit, this));		
	}
	
	/**
	 * Returns the text edit changes managed by this text change.
	 * 
	 * @return the text edit changes
	 */
	public EditChange[] getTextEditChanges() {
		return (EditChange[])fTextEditChanges.toArray(new EditChange[fTextEditChanges.size()]);
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
		return getPreviewTextBuffer().getContent();
	}
	
	/**
	 * Note: API is under construction
	 */
	public TextBuffer getPreviewTextBuffer() throws JavaModelException {
		try {
			TextBufferEditor editor= new TextBufferEditor(createTextBuffer());
			addTextEdits(editor, true);
			editor.performEdits(new NullProgressMonitor());
			return editor.getTextBuffer();
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
	public String getCurrentContent(EditChange change, int surroundingLines) throws CoreException {
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
	public String getPreviewContent(EditChange change, int surroundingLines) throws CoreException {
		return getContent(change, surroundingLines, true);
	}

	/**
	 * Controls whether the text change should track position changes. If a change has been
	 * executed a call to <code>getNewTextRange(TextEdit)</code> will return the text edit's
	 * text range in the modified document.
	 * 
	 * @param track if <code>true</code> track text position changes.
	 */
	public void setTrackPositionChanges(boolean track) {
		if (track)
			fPositionMap= new HashMap(fTextEditChanges.size() * 2);
		else
			fPositionMap= null;
	}
	
	/**
	 * Returns the text range of the given text edit. If the change doesn't manage the given
	 * text edit or if <code>setTrackPositionChanges</code> is set to <code>false</code>
	 * <code>null</code> is returned.
	 * 
	 * <p>
	 * Note: API is under construction
	 * </P>
	 */
	public TextRange getNewTextRange(TextEdit edit) {
		if (fPositionMap == null)
			return null;
		TextEdit result= (TextEdit)fPositionMap.get(edit);
		if (result == null)
			return null;
		return result.getTextRange().copy();
	}
	
	/**
	 * Note: API is under construction
	 */
	public TextRange getNewTextRange(EditChange editChange) {
		Object edit= editChange.getEdit();
		if (edit instanceof TextEdit)
			return getNewTextRange((TextEdit)edit);
		return null;
	}
	
	/* (Non-Javadoc)
	 * Method declared in IChange.
	 */
	public void setActive(boolean active) {
		super.setActive(active);
		for (Iterator iter= fTextEditChanges.iterator(); iter.hasNext();) {
			EditChange element= (EditChange) iter.next();
			element.setActive(active);
		}
	}
	
	/* non Java-doc
	 * @see AbstractTextChange#addTextEdits
	 */
	protected void addTextEdits(TextBufferEditor editor, boolean copy) throws CoreException {
		for (Iterator iter= fTextEditChanges.iterator(); iter.hasNext(); ) {
			EditChange edit= (EditChange)iter.next();
			if (edit.isActive()) {
				edit.addTo(editor, copy, fPositionMap);
			}
		}		
	}
	
	protected Map getPositionMap() {
		return fPositionMap;
	}
	
	private String getContent(EditChange change, int surroundingLines, boolean preview) throws CoreException {
		Assert.isTrue(change.getTextChange() == this);
		TextBuffer buffer= createTextBuffer();
		if (preview) {
			TextBufferEditor editor= new TextBufferEditor(buffer);
			change.addTo(editor, true, fPositionMap);
			editor.performEdits(new NullProgressMonitor());
		}
		TextRange range= change.getTextRange();
		int startLine= Math.max(buffer.getLineOfOffset(range.getOffset()) - surroundingLines, 0);
		int endLine= Math.min(
			buffer.getLineOfOffset(range.getInclusiveEnd()) + surroundingLines,
			buffer.getNumberOfLines() - 1);
		int offset= buffer.getLineInformation(startLine).getOffset();
		TextRegion region= buffer.getLineInformation(endLine);
		int length = region.getOffset() + region.getLength() - offset;
		return buffer.getContent(offset, length);
	}	
}

