/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.actions;

import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPartitioningException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITypedRegion;

import org.eclipse.ui.IEditorInput;

import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.ui.text.IJavaPartitions;


/**
 * Action that removes the enclosing comment marks from a Java block comment.
 *
 * @since 3.0
 */
public class RemoveBlockCommentAction extends BlockCommentAction {

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
	 * @see org.eclipse.jdt.internal.ui.actions.AddBlockCommentAction#runInternal(org.eclipse.jface.text.ITextSelection, org.eclipse.jface.text.IDocumentExtension3, org.eclipse.jdt.internal.ui.actions.AddBlockCommentAction.Edit.EditFactory)
	 */
	@Override
	protected void runInternal(ITextSelection selection, IDocumentExtension3 docExtension, Edit.EditFactory factory) throws BadPartitioningException, BadLocationException {
		List<Edit> edits= new LinkedList<>();
		int tokenLength= getCommentStart().length();

		int offset= selection.getOffset();
		int endOffset= offset + selection.getLength();

		ITypedRegion partition= docExtension.getPartition(IJavaPartitions.JAVA_PARTITIONING, offset, false);
		int partOffset= partition.getOffset();
		int partEndOffset= partOffset + partition.getLength();

		while (partEndOffset < endOffset) {

			if (partition.getType() == IJavaPartitions.JAVA_MULTI_LINE_COMMENT) {
				// remove first '/*'
				edits.add(factory.createEdit(partOffset, tokenLength, "")); //$NON-NLS-1$
				// remove first '*' from each line after
				removeToken(partition, partOffset, factory, edits);
				// remove last '*/'
				edits.add(factory.createEdit(partEndOffset - tokenLength, tokenLength, "")); //$NON-NLS-1$
			}

			partition= docExtension.getPartition(IJavaPartitions.JAVA_PARTITIONING, partEndOffset, false);
			partOffset= partition.getOffset();
			partEndOffset= partOffset + partition.getLength();
		}

		if (partition.getType() == IJavaPartitions.JAVA_MULTI_LINE_COMMENT) {
			// remove first '/*'
			edits.add(factory.createEdit(partOffset, tokenLength, "")); //$NON-NLS-1$
			// remove first '*' from each line after
			removeToken(partition, partOffset, factory, edits);
			// remove last '*/'
			edits.add(factory.createEdit(partEndOffset - tokenLength, tokenLength, "")); //$NON-NLS-1$
		}

		executeEdits(edits);
	}

	/*
	 * Remove the first '*' token from each line if it is the first character, from block comment.
	 */
	private void removeToken(final ITypedRegion partition, int partOffset, final Edit.EditFactory factory, final List<Edit> edits) throws BadLocationException {
		IEditorInput editorInput = getTextEditor().getEditorInput();
		IDocument document = getTextEditor().getDocumentProvider().getDocument(editorInput);
		int startLine = document.getLineOfOffset(partOffset);
		int lines = document.getNumberOfLines(partOffset, partition.getLength());

		for(int line = startLine; line < startLine + lines; line++) {
			int lineOffset = document.getLineOffset(line);
			int lineLength = document.getLineLength(line);
			char ch1 = document.getChar(lineOffset);
			int pos = 0;
			// skip all character codes <= 0x20 from start of line
			while(pos < lineLength && ((ch1 = document.getChar(lineOffset + pos)) <= 0x20)) {
				pos++;
			}
			if(ch1 == '*') {
				/* next char after '*' must NOT be a '/', this is handled separately */
				if(pos + 1 < lineLength) {
					char ch2 = document.getChar(lineOffset + pos + 1);
					if(ch2 == '/') {
						continue; // do next line
					}
				}
				Edit edit= factory.createEdit(lineOffset + pos, 1, ""); //$NON-NLS-1$
				edits.add(edit);
			}
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.actions.AddBlockCommentAction#validSelection(org.eclipse.jface.text.ITextSelection)
	 */
	@Override
	protected boolean isValidSelection(ITextSelection selection) {
		return selection != null && !selection.isEmpty();
	}
}
