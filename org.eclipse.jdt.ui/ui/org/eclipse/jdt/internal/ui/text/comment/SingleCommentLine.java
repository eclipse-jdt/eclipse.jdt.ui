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

/**
 * Single-line comment line in a comment region.
 * 
 * @since 3.0
 */
public class SingleCommentLine extends CommentLine {

	/** Line prefix for single line comments */
	private static final String SINGLE_COMMENT_PREFIX= "// "; //$NON-NLS-1$

	/**
	 * Creates a new single-line comment line.
	 * 
	 * @param region Comment region to create the line for
	 * @param range Range of the line in the underlying text store measured in comment region coordinates 
	 */
	public SingleCommentLine(CommentRegion region, CommentRange range) {
		super(region, range);
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentLine#getContentLinePrefix()
	 */
	protected String getContentLinePrefix() {
		return SINGLE_COMMENT_PREFIX;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentLine#getEndLinePrefix()
	 */
	protected String getEndLinePrefix() {
		return SINGLE_COMMENT_PREFIX;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentLine#getLinePrefix(int)
	 */
	protected String getLinePrefix(int index) {
		return getContentLinePrefix();
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentLine#getStartLinePrefix()
	 */
	protected String getStartLinePrefix() {
		return SINGLE_COMMENT_PREFIX;
	}
}
