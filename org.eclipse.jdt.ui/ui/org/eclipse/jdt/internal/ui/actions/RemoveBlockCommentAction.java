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
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextUtilities;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorExtension2;
import org.eclipse.ui.texteditor.TextEditorAction;

import org.eclipse.jdt.internal.core.Assert;
import org.eclipse.jdt.internal.ui.text.IJavaPartitions;

/**
 * Action that removes the enclosing comment marks from a Java block comment.
 * 
 * @since 3.0
 */
public class RemoveBlockCommentAction extends TextEditorAction {

	/**
	 * Creates a new instance.
	 * 
	 * @param bundle the resource bundle
	 * @param prefix a prefix to be prepended to the various resource keys
	 *   (described in <code>ResourceAction</code> constructor), or 
	 *   <code>null</code> if none
	 * @param editor the text editor
	 */
	public RemoveBlockCommentAction(ResourceBundle bundle, String prefix, ITextEditor editor) {
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
		if (selection == null || selection.isEmpty())
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
			ITypedRegion region= getBlockCommentRegion(selection);
			if (region != null) {
				int offset= region.getOffset();
				document.replace(offset, 2,	"");//$NON-NLS-1$
				document.replace(offset + region.getLength() - 4, 2, ""); //$NON-NLS-1$
			}
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

	/*
	 * @see org.eclipse.ui.texteditor.TextEditorAction#update()
	 */
	public void update() {
		super.update();

//	does not work, since only selection updates are heard, but not caret moves		
//		if (isEnabled()) {
//			if (getBlockCommentRegion(getCurrentSelection()) == null)
//				setEnabled(false);
//		}
	}
	
	/**
	 * Returns the block comment typed region enclosing the position at the end of <code>selection</code> or
	 * <code>null</code> if there is no block comment at this position.
	 * 
	 * @param selection the caret position (the end of the selection is taken as the position)
	 * @return the block comment region at the selection's end, or <code>null</code>
	 */
	private ITypedRegion getBlockCommentRegion(ITextSelection selection) {
		ITextEditor editor= getTextEditor();
		if (editor == null)
			return null;
			
		IDocumentProvider docProvider= editor.getDocumentProvider();
		IEditorInput input= editor.getEditorInput();
		if (docProvider == null || input == null)
			return null;
			
		IDocument document= docProvider.getDocument(input);
		if (document == null)
			return null;
			
		try {
			
			ITypedRegion region= TextUtilities.getPartition(document, IJavaPartitions.JAVA_PARTITIONING, selection.getOffset() + selection.getLength());
			if (region != null && region.getType() == IJavaPartitions.JAVA_MULTI_LINE_COMMENT)
				return region;
				
		} catch (BadLocationException e) {
			// can happen on concurrent modification, deletion etc. of the document 
			// don't complain, just bail out
			// don't force the endCompoundChange either, since another modification is probably
			// going on.
		}
		
		return null;
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
