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
	private static final String MULTI_COMMENT_CONTENT_PREFIX= " * "; //$NON-NLS-1$

	/** Line prefix of multi-line comment end lines */
	private static final String MULTI_COMMENT_END_PREFIX= " */"; //$NON-NLS-1$

	/** Line prefix of multi-line comment content lines */
	private static final String MULTI_COMMENT_START_PREFIX= "/* "; //$NON-NLS-1$

	/**
	 * Creates a new multi-line comment line.
	 * 
	 * @param region Comment region to create the line for
	 * @param range Range of the line in the underlying text store measured in comment region coordinates 
	 */
	public MultiCommentLine(CommentRegion region, CommentRange range) {
		super(region, range);
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentLine#getContentLinePrefix()
	 */
	protected String getContentLinePrefix() {
		return MULTI_COMMENT_CONTENT_PREFIX;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentLine#getEndLinePrefix()
	 */
	protected String getEndLinePrefix() {
		return MULTI_COMMENT_END_PREFIX;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentLine#getStartLinePrefix()
	 */
	protected String getStartLinePrefix() {
		return MULTI_COMMENT_START_PREFIX;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentLine#scanLine(int)
	 */
	public boolean scanLine(int index) {

		final String contentPrefix= getContentLinePrefix().trim();
		final String endPrefix= getEndLinePrefix().trim();

		final CommentRange range= (CommentRange)fRanges.get(0);
		String content= fRegion.getContent(range);

		// Search for end prefix
		if (index == fRegion.getLineCount() - 1) {

			// Search for lower border
			final int contentOffset= content.indexOf(contentPrefix);
			final int endOffset= content.indexOf(endPrefix);

			if (endOffset >= 0) {
				if (endOffset == contentOffset) {

					// Leave text content
					range.setLength(endOffset);
				} else {

					// Trim to text content
					range.changeOffset(contentOffset);
					content= content.substring(contentOffset);

					final IRegion result= trimLine(content, contentPrefix);
					range.changeOffset(result.getOffset());
					range.setLength(result.getLength());

					fRegion.setLowerBorder();
				}
				return true;
			}
		}

		// Search for other prefix
		else if (super.scanLine(index)) {

			// Search for upper border
			content= fRegion.getContent(range);
			if (index == 0 && content.endsWith(contentPrefix)) {

				// Trim to text content
				final IRegion result= trimLine(content, contentPrefix);
				range.changeOffset(result.getOffset());
				range.setLength(result.getLength());

				fRegion.setUpperBorder();
			}

			// Search for right border				
			else if (index > 0 && index < fRegion.getLineCount() - 1 && content.endsWith(contentPrefix)) {
				range.changeLength(-contentPrefix.length());

				fRegion.setRightBorder();
			}
			return true;
		}
		return false;
	}

	/**
	 * Removes all leading and trailing occurrences from <code>string</code>.
	 * 
	 * @param content The string to remove the occurrences of <code>remove</code>
	 * @param remove The string to remove from <code>string</code>
	 * @return The region of the trimmed substring within <code>string</code>
	 */
	protected IRegion trimLine(String string, String remove) {

		int offset= 0;

		final int length= remove.length();
		final int total= string.length();

		while (offset < total && string.startsWith(remove, offset))
			offset += length;

		if (offset < total - length) {
			int index= string.indexOf(remove, offset + 1);
			if (index >= offset + 1)
				return new Region(offset, index - offset);
		}
		return new Region(0, 0);
	}
}
