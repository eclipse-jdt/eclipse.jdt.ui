/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.changes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.textmanipulation.AutoOrganizingTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.GroupDescription;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBufferEditor;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEditCopier;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRegion;

public abstract class TextChange extends AbstractTextChange {

	public static class EditChange {
		private boolean fIsActive;
		private TextChange fTextChange;
		private GroupDescription fDescription;
		
		/* package */ EditChange(GroupDescription description, TextChange change) {
			fTextChange= change;
			fIsActive= true;
			fDescription= description;
		}
		public String getName() {
			return fDescription.getName();
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
		public TextRange getTextRange() {
			return fDescription.getTextRange();
		}
		public Object getModifiedElement() {
			return fDescription.getModifiedElement();
		}
		/* package */ GroupDescription getGroupDescription() {
			return fDescription;
		}
	}

	private List fTextEditChanges;
	private TextEditCopier fCopier;
	private TextEdit fEdit;
	private boolean fKeepExecutedTextEdits;
	private boolean fAutoMode;

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
		addTextEdit(name, new TextEdit[] {edit});
	}
	
	/**
	 * Adds an array of  text edit objects to this text buffer change.
	 *
	 * @param name the name of the given text edit. The name is used to render this 
	 * 	change in the UI.
	 * @param edite the array of text edits to add
	 */
	public void addTextEdit(String name, TextEdit[] edits) {
		Assert.isNotNull(name);
		Assert.isNotNull(edits);
		GroupDescription description= new GroupDescription(name, edits);
		fTextEditChanges.add(new EditChange(description, this));
		if (fEdit == null) {
			fEdit= new AutoOrganizingTextEdit();
			fAutoMode= true;
		} else {
			Assert.isTrue(fAutoMode, "Can only add edits when in auot organizing mode"); //$NON-NLS-1$
		}
		for (int i= 0; i < edits.length; i++) {
			fEdit.add(edits[i]);
		}
	}
	
	/**
	 * Sets the root text edit.
	 * 
	 * @param edit the root text edit
	 */
	public void setEdit(TextEdit edit) {
		Assert.isTrue(fEdit == null, "Root edit can only be set once"); //$NON-NLS-1$
		Assert.isTrue(edit != null);
		fEdit= edit;
		fTextEditChanges=  new ArrayList(5);
		fAutoMode= false;
	}
	
	/**
	 * Gets the root text edit.
	 * 
	 * @return Returns the root text edit
	 */
	public TextEdit getEdit() {
		return fEdit;
	}	
	
	/**
	 * Adds a group description.
	 * 
	 * @param description the group description to be added
	 */
	public void addGroupDescription(GroupDescription description) {
		Assert.isTrue(fEdit != null, "Can only add a description if a root edit exists"); //$NON-NLS-1$
		Assert.isTrue(!fAutoMode, "Group descriptions are only supported if root edit has been set by setEdit"); //$NON-NLS-1$
		Assert.isTrue(description != null);
		fTextEditChanges.add(new EditChange(description, this));
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
			addTextEdits(editor);
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
	 * Controls whether the text change should keep executed edits. If set to <code>true</code>
	 * a call to <code>getExecutedTextEdit(TextEdit original)</code> will return the executed edit
	 * associated with the original edit.
	 * 
	 * @param keep if <code>true</code> executed edits are kept
	 */
	public void setKeepExecutedTextEdits(boolean keep) {
		fKeepExecutedTextEdits= keep;
	}
	
	/**
	 * Returns the edit that got copied and executed instead of the orignial.
	 * 
	 * @return the executed edit
	 */
	public TextEdit getExecutedTextEdit(TextEdit original) {
		if (!fKeepExecutedTextEdits || fCopier == null)
			return null;
		return fCopier.getCopy(original);
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
		TextEdit result= getExecutedTextEdit(edit);
		if (result == null)
			return null;
		return result.getTextRange().copy();
	}
	
	/**
	 * Note: API is under construction
	 */
	public TextRange getNewTextRange(EditChange editChange) {
		if (!fKeepExecutedTextEdits || fCopier == null)
			return null;
		TextEdit[] edits= editChange.getGroupDescription().getTextEdits();
		List copies= new ArrayList(edits.length);
		for (int i= 0; i < edits.length; i++) {
			copies.add(fCopier.getCopy(edits[i]));
		}
		return TextEdit.getTextRange(copies);
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
	protected void addTextEdits(TextBufferEditor editor) throws CoreException {
		if (fEdit == null)
			return;
		fCopier= new TextEditCopier(fEdit);
		TextEdit copiedEdit= fCopier.copy();
		for (Iterator iter= fTextEditChanges.iterator(); iter.hasNext(); ) {
			EditChange edit= (EditChange)iter.next();
			if (!edit.isActive()) {
				TextEdit[] edits= edit.getGroupDescription().getTextEdits();
				for (int i= 0; i < edits.length; i++) {
					TextEdit textEdit= fCopier.getCopy(edits[i]);
					if (textEdit != null)
						textEdit.setActive(false);
				}
			}
		}
		editor.add(copiedEdit);		
		if (!fKeepExecutedTextEdits)
			fCopier= null;
	}
	
	protected void addTextEdits(TextBufferEditor editor, EditChange[] changes) throws CoreException {
		if (fEdit == null)
			return;
		fCopier= new TextEditCopier(fEdit);
		TextEdit copiedEdit= fCopier.copy();
		copiedEdit.setActive(false);
		for (int c= 0; c < changes.length; c++) {
			EditChange change= changes[c];
			Assert.isTrue(change.getTextChange() == this);
			boolean active= change.isActive();
			TextEdit[] edits= change.getGroupDescription().getTextEdits();
			for (int e= 0; e < edits.length; e++) {
				fCopier.getCopy(edits[e]).setActive(active);
			}
		}
		editor.add(copiedEdit);		
		if (!fKeepExecutedTextEdits)
			fCopier= null;
	}
	
	private String getContent(EditChange change, int surroundingLines, boolean preview) throws CoreException {
		Assert.isTrue(change.getTextChange() == this);
		TextBuffer buffer= createTextBuffer();
		if (preview) {
			TextBufferEditor editor= new TextBufferEditor(buffer);
			addTextEdits(editor, new EditChange[] { change });
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

