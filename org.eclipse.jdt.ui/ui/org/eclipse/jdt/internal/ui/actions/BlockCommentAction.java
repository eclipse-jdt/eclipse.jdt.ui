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
package org.eclipse.jdt.internal.ui.actions;

import java.util.ResourceBundle;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorExtension2;
import org.eclipse.ui.texteditor.TextEditorAction;

import org.eclipse.jdt.internal.core.Assert;

/**
 * Action that encloses the editor's current selection with Java block comment terminators
 * (<code>&#47;&#42;</code> and <code>&#42;&#47;</code>).
 * 
 * @since 3.0
 */
public class BlockCommentAction extends TextEditorAction {

	/**
	 * Creates a new instance.
	 * 
	 * @param bundle the resource bundle
	 * @param prefix a prefix to be prepended to the various resource keys
	 *   (described in <code>ResourceAction</code> constructor), or 
	 *   <code>null</code> if none
	 * @param editor the text editor
	 */
	public BlockCommentAction(ResourceBundle bundle, String prefix, ITextEditor editor) {
		super(bundle, prefix, editor);
	}
	
	/*
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {
		if (!isEnabled())
			return;
			
		ITextEditor editor= getTextEditor();
		if (editor == null || !ensureEditable(editor))
			return;
			
		ITextSelection selection= getCurrentSelection();
		if (!validSelection(selection))
			return;
		
		IDocumentProvider docProvider= editor.getDocumentProvider();
		IEditorInput input= editor.getEditorInput();
		if (docProvider == null || input == null)
			return;
			
		IDocument document= docProvider.getDocument(input);
		if (document == null)
			return;
		
		IRewriteTarget target= (IRewriteTarget)editor.getAdapter(IRewriteTarget.class);
		if (target != null) {
			target.beginCompoundChange();
		}
		
		try {
			int offset= selection.getOffset();
			String start= getCommentStart();
			document.replace(offset, 0, start);
			document.replace(offset + selection.getLength() + start.length(), 0, getCommentEnd());
		} catch (BadLocationException e) {
			// can happen on concurrent modification, deletion etc. of the document 
			// -> don't complain, just bail out
		} finally {
			if (target != null) {
				target.endCompoundChange();
			}
		}
	}
	
	/**
	 * Ensures that the editor is modifyable. If the editor is an instance of
	 * <code>ITextEditorExtension2</code>, its <code>validateEditorInputState</code> method 
	 * is called, otherwise, the result of <code>isEditable</code> is returned.
	 * 
	 * @param editor the editor to be checked
	 * @return <code>true</code> if the editor is editable, <code>false</code> otherwise
	 */
	private boolean ensureEditable(ITextEditor editor) {
		Assert.isNotNull(editor);

		if (editor instanceof ITextEditorExtension2) {
			ITextEditorExtension2 ext= (ITextEditorExtension2) editor;
			return ext.validateEditorInputState();
		}
		
		return editor.isEditable();
	}

	/**
	 * Returns the text to be inserted at the selection start.
	 * 
	 * @return the text to be inserted at the selection start
	 */
	protected String getCommentStart() {
		// for now: no space story
		return "/*";
	}

	/**
	 * Returns the text to be inserted at the selection end.
	 * 
	 * @return the text to be inserted at the selection end
	 */
	protected String getCommentEnd() {
		// for now: no space story
		return "*/";
	}

	/*
	 * @see org.eclipse.ui.texteditor.TextEditorAction#update()
	 */
	public void update() {
		super.update();
		
		if (isEnabled()) {
			if (!validSelection(getCurrentSelection()))
				setEnabled(false);
		}
	}
	
	/**
	 * Checks whether <code>selection</code> is valid, i.e. neither <code>null</code> or empty.
	 * 
	 * @param selection the selection to check
	 * @return <code>true</code> if the selection is valid, <code>false</code> otherwise
	 */
	private boolean validSelection(ITextSelection selection) {
		return selection != null && !selection.isEmpty() && selection.getLength() > 0;
	}

	/**
	 * Returns the editor's selection, or <code>null</code> if no selection can be obtained or the 
	 * editor is <code>null</code>.
	 * 
	 * @return the selection of the action's editor, or <code>null</code>
	 */
	private ITextSelection getCurrentSelection() {
		ITextEditor editor= getTextEditor();
		if (editor != null) {
			ISelectionProvider provider= editor.getSelectionProvider();
			if (provider != null) {
				ISelection selection= provider.getSelection();
				if (selection instanceof ITextSelection) 
					return (ITextSelection) selection;
			}
		}
		return null;
	}

}
