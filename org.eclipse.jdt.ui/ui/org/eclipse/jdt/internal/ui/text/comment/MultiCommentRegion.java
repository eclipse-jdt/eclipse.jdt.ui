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

import org.eclipse.jface.text.TypedPosition;

/**
 * Multi-comment region in a source code document.
 * 
 * @since 3.0
 */
public class MultiCommentRegion extends CommentRegion {

	/**
	 * Creates a new multi-comment region.
	 * 
	 * @param strategy The comment formatting strategy used to format this comment region
	 * @param position The typed position which forms this comment region
	 * @param delimiter The line delimiter to use in this comment region
	 */
	protected MultiCommentRegion(final CommentFormattingStrategy strategy, final TypedPosition position, final String delimiter) {
		super(strategy, position, delimiter);
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentRegion#canAppend(org.eclipse.jdt.internal.ui.text.comment.CommentLine, org.eclipse.jdt.internal.ui.text.comment.CommentRange, org.eclipse.jdt.internal.ui.text.comment.CommentRange, int, int)
	 */
	protected boolean canAppend(final CommentLine line, final CommentRange previous, final CommentRange next, final int space, final int length) {

		if (previous != null) {

			final String content= getText(previous.getOffset(), previous.getLength());

			for (int index= 0; index < IJavaDocTagConstants.JAVADOC_REFERENCE_TAGS.length; index++) {
				if (content.equals(IJavaDocTagConstants.JAVADOC_REFERENCE_TAGS[index]))
					return true;
			}
		}
		return super.canAppend(line, previous, next, space, length);
	}
}
