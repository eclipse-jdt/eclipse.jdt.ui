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
 * Javadoc comment line in a comment region.
 * 
 * @since 3.0
 */
public class JavaDocLine extends MultiCommentLine {

	/** Line prefix of javadoc start lines */
	public static final String JAVADOC_START_PREFIX= "/**"; //$NON-NLS-1$

	/**
	 * Creates a new javadoc line.
	 * 
	 * @param region
	 *                  Comment region to create the line for
	 */
	protected JavaDocLine(final CommentRegion region) {
		super(region);
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentLine#applyStart(org.eclipse.jdt.internal.ui.text.comment.CommentRange,java.lang.String, int)
	 */
	protected void applyStart(final CommentRange range, final String indentation, final int length) {

		final CommentRegion parent= getParent();

		if (parent.isSingleLine() && parent.getSize() == 1) {
			parent.applyText(getStartingPrefix() + CommentRegion.COMMENT_RANGE_DELIMITER, 0, range.getOffset());
		} else
			super.applyStart(range, indentation, length);
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentLine#getStartingPrefix()
	 */
	protected String getStartingPrefix() {
		return JAVADOC_START_PREFIX;
	}
}
