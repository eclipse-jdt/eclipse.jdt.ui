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

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

/**
 * Multi-line comment line in a comment region.
 * 
 * @since 3.0
 */
public class MultiCommentLine extends CommentLine {

	/** Line prefix of multi-line comment content lines */
	public static final String MULTI_COMMENT_CONTENT_PREFIX= " * "; //$NON-NLS-1$

	/** Line prefix of multi-line comment end lines */
	public static final String MULTI_COMMENT_END_PREFIX= " */"; //$NON-NLS-1$

	/** Line prefix of multi-line comment content lines */
	public static final String MULTI_COMMENT_START_PREFIX= "/* "; //$NON-NLS-1$

	/**
	 * Creates a new multi-line comment line.
	 * 
	 * @param region Comment region to create the line for
	 */
	protected MultiCommentLine(final CommentRegion region) {
		super(region);
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentLine#adapt(org.eclipse.jdt.internal.ui.text.comment.CommentLine)
	 */
	protected void adapt(final CommentLine previous) {
		// Do nothing
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentLine#getContentLinePrefix()
	 */
	protected String getContentPrefix() {
		return MULTI_COMMENT_CONTENT_PREFIX;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentLine#getEndLinePrefix()
	 */
	protected String getEndingPrefix() {
		return MULTI_COMMENT_END_PREFIX;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentLine#getStartLinePrefix()
	 */
	protected String getStartingPrefix() {
		return MULTI_COMMENT_START_PREFIX;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentLine#scanLine(int)
	 */
	protected void scanLine(final int line) {

		final CommentRegion parent= getParent();
		final String start= getStartingPrefix().trim();
		final String end= getEndingPrefix().trim();
		final String content= getContentPrefix().trim();

		final int lines= parent.getSize();
		final CommentRange range= getFirst();

		int offset= 0;
		int postfix= 0;

		String text= parent.getText(range.getOffset(), range.getLength());
		if (line == 0) {

			offset= text.indexOf(start);
			if (offset >= 0) {

				offset += start.length();
				range.trimBegin(offset);

				postfix= text.lastIndexOf(content);
				if (postfix >= offset) {

					range.setLength(postfix - offset);
					parent.setBorder(BORDER_UPPER);

					if (postfix > offset) {

						text= parent.getText(range.getOffset(), range.getLength());
						final IRegion region= trimLine(text, content);

						range.move(region.getOffset());
						range.setLength(region.getLength());
					}
				}
			}
		} else if (line == lines - 1) {

			offset= text.indexOf(content);
			if (offset >= 0) {

				if (text.startsWith(end, offset)) {

					range.move(offset);
					range.setLength(0);

				} else {

					postfix= text.lastIndexOf(end);
					if (postfix > offset) {

						text= parent.getText(range.getOffset(), range.getLength());
						final IRegion region= trimLine(text, content);

						range.move(region.getOffset());
						range.setLength(region.getLength());

						parent.setBorder(BORDER_UPPER);
						parent.setBorder(BORDER_LOWER);
					}
				}
			}
		} else {

			offset= text.indexOf(content);
			if (offset >= 0) {

				offset += content.length();
				range.trimBegin(offset);
			}
		}
	}

	/**
	 * Removes all leading and trailing occurrences from <code>line</code>.
	 * 
	 * @param line The string to remove the occurrences of <code>trimmable</code>
	 * @param trimmable The string to remove from <code>line</code>
	 * @return The region of the trimmed substring within <code>line</code>
	 */
	protected IRegion trimLine(final String line, final String trimmable) {

		int offset= 0;

		final int length= trimmable.length();
		final int total= line.length();

		while (offset < total && line.startsWith(trimmable, offset))
			offset += length;

		if (offset < total - length) {

			final int index= line.indexOf(trimmable, offset + 1);
			if (index >= offset + 1)
				return new Region(offset, index - offset);
		}
		return new Region(0, 0);
	}
}
