/*****************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/

package org.eclipse.jdt.internal.ui.text.comment;

import org.eclipse.jdt.internal.corext.refactoring.nls.NLSElement;

/**
 * Single-line comment line in a comment region.
 * 
 * @since 3.0
 */
public class SingleCommentLine extends CommentLine {

	/** Line prefix for single line comments */
	public static final String SINGLE_COMMENT_PREFIX= "// "; //$NON-NLS-1$
	
	/** Is the comment a NLS tag sequence? */
	private boolean fNlsSequence= false;

	/**
	 * Creates a new single-line comment line.
	 * 
	 * @param region Comment region to create the line for
	 */
	protected SingleCommentLine(final CommentRegion region) {
		super(region);
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentLine#adapt(org.eclipse.jdt.internal.ui.text.comment.CommentLine)
	 */
	protected void adapt(final CommentLine previous) {
		// Do nothing
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentLine#applyEnd(org.eclipse.jdt.internal.ui.text.comment.CommentRange, java.lang.String, int)
	 */
	protected void applyEnd(final CommentRange range, final String indentation, final int length) {

		final int offset= range.getOffset() + range.getLength();
		final CommentRegion parent= getParent();

		parent.applyText(parent.getDelimiter(), offset, parent.getLength() - offset);
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentLine#applyStart(org.eclipse.jdt.internal.ui.text.comment.CommentRange, java.lang.String, int)
	 */
	protected void applyStart(final CommentRange range, final String indentation, final int length) {

		final CommentRegion parent= getParent();

		parent.applyText(getContentPrefix(), 0, range.getOffset() - parent.getOffset());
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentLine#getContentLinePrefix()
	 */
	protected String getContentPrefix() {
		return SINGLE_COMMENT_PREFIX;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentLine#getEndLinePrefix()
	 */
	protected String getEndingPrefix() {
		return SINGLE_COMMENT_PREFIX;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentLine#getStartLinePrefix()
	 */
	protected String getStartingPrefix() {
		return SINGLE_COMMENT_PREFIX;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentLine#scanLine(int)
	 */
	protected void scanLine(final int line) {

		final CommentRange range= getFirst();
		final String content= getParent().getText(range.getOffset(), range.getLength());
		final String prefix= getContentPrefix().trim();

		final int offset= content.indexOf(prefix);
		if (offset >= 0) {
			
			if (content.startsWith(NLSElement.TAG_PREFIX))
				fNlsSequence= true;
			
			range.trimBegin(offset + prefix.length());
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentLine#tokenizeLine(int)
	 */
	protected void tokenizeLine(final int line) {
		if (!fNlsSequence)
			super.tokenizeLine(line);
	}
}
