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
public class MultiCommentLine extends CommentLine implements ICommentAttributes, IHtmlTagConstants, ILinkTagConstants {

	/** Line prefix of multi-line comment content lines */
	public static final String MULTI_COMMENT_CONTENT_PREFIX= " * "; //$NON-NLS-1$

	/** Line prefix of multi-line comment end lines */
	public static final String MULTI_COMMENT_END_PREFIX= " */"; //$NON-NLS-1$

	/** Line prefix of multi-line comment content lines */
	public static final String MULTI_COMMENT_START_PREFIX= "/* "; //$NON-NLS-1$

	/** The reference indentation of this line */
	private String fIndentation= ""; //$NON-NLS-1$

	/**
	 * Creates a new multi-line comment line.
	 * 
	 * @param region
	 *                  Comment region to create the line for
	 */
	protected MultiCommentLine(final CommentRegion region) {
		super(region);
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentLine#adapt(org.eclipse.jdt.internal.ui.text.comment.CommentLine)
	 */
	protected void adapt(final CommentLine previous) {

		if (!hasAttribute(COMMENT_ROOT) && !hasAttribute(COMMENT_PARAMETER) && !previous.hasAttribute(COMMENT_BLANKLINE))
			fIndentation= previous.getIndentation();
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentLine#append(org.eclipse.jdt.internal.ui.text.comment.CommentRange)
	 */
	protected void append(final CommentRange range) {

		final MultiCommentRegion parent= (MultiCommentRegion)getParent();

		if (range.hasAttribute(COMMENT_PARAMETER))
			setAttribute(COMMENT_PARAMETER);
		else if (range.hasAttribute(COMMENT_ROOT))
			setAttribute(COMMENT_ROOT);
		else if (range.hasAttribute(COMMENT_BLANKLINE))
			setAttribute(COMMENT_BLANKLINE);

		final int ranges= getSize();
		if (ranges == 1) {

			if (parent.isIndentRoots()) {

				final CommentRange first= getFirst();
				final String common= parent.getText(first.getOffset(), first.getLength()) + CommentRegion.COMMENT_RANGE_DELIMITER;

				if (hasAttribute(COMMENT_ROOT))
					fIndentation= common;
				else if (hasAttribute(COMMENT_PARAMETER)) {
					if (parent.isIndentDescriptions())
						fIndentation= common + "\t"; //$NON-NLS-1$
					else
						fIndentation= common;
				}
			}
		}
		super.append(range);
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

	/**
	 * Returns the reference indentation to use for this line.
	 * 
	 * @return The reference indentation for this line
	 */
	protected final String getIndentation() {
		return fIndentation;
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

				postfix= text.lastIndexOf(end);
				if (postfix > offset)
					range.setLength(postfix - offset);
				else {
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
			}
		} else if (line == lines - 1) {

			offset= text.indexOf(content);
			if (offset >= 0) {

				range.trimBegin(offset + 1);
				if (text.startsWith(end, offset))
					range.setLength(0);
				else {

					postfix= text.lastIndexOf(end);
					if (postfix > offset) {

						range.trimEnd(-end.length());
						text= parent.getText(range.getOffset(), range.getLength());

						final IRegion region= trimLine(text, content);
						if (region.getOffset() != 0 || region.getLength() != text.length()) {

							range.move(region.getOffset());
							range.setLength(region.getLength());

							parent.setBorder(BORDER_UPPER);
							parent.setBorder(BORDER_LOWER);
						}
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

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentLine#tokenizeLine(int)
	 */
	protected void tokenizeLine(int line) {

		int offset= 0;
		int index= offset;

		final CommentRegion parent= getParent();
		final CommentRange range= getFirst();
		final int begin= range.getOffset();

		final String content= parent.getText(begin, range.getLength());
		final int length= content.length();

		while (offset < length && Character.isWhitespace(content.charAt(offset)))
			offset++;

		CommentRange result= null;
		if (offset >= length && !parent.isClearLines() && (line > 0 && line < parent.getSize() - 1)) {

			result= new CommentRange(begin, 0);
			result.setAttribute(COMMENT_BLANKLINE);

			parent.append(result);
		}

		int attribute= 0;
		while (offset < length) {

			while (offset < length && Character.isWhitespace(content.charAt(offset)))
				offset++;

			attribute= 0;
			index= offset;

			if (index < length) {

				if (content.charAt(index) == HTML_TAG_PREFIX) {

					while (index < length && content.charAt(index) != HTML_TAG_POSTFIX)
						index++;

					if (index < length && content.charAt(index) == HTML_TAG_POSTFIX)
						index++;

					attribute= COMMENT_HTML;

				} else if (content.startsWith(LINK_TAG_PREFIX, index)) {

					while (index < length && content.charAt(index) != LINK_TAG_POSTFIX)
						index++;

					if (index < length && content.charAt(index) == LINK_TAG_POSTFIX)
						index++;

					attribute= COMMENT_OPEN | COMMENT_CLOSE;

				} else {

					while (index < length && !Character.isWhitespace(content.charAt(index)) && content.charAt(index) != HTML_TAG_PREFIX && !content.startsWith(LINK_TAG_PREFIX, index))
						index++;
				}
			}

			if (index - offset > 0) {

				result= new CommentRange(begin + offset, index - offset);
				result.setAttribute(attribute);

				parent.append(result);
				offset= index;
			}
		}
	}

	/**
	 * Removes all leading and trailing occurrences from <code>line</code>.
	 * 
	 * @param line
	 *                  The string to remove the occurrences of <code>trimmable</code>
	 * @param trimmable
	 *                  The string to remove from <code>line</code>
	 * @return The region of the trimmed substring within <code>line</code>
	 */
	protected final IRegion trimLine(final String line, final String trimmable) {

		final int trim= trimmable.length();

		int offset= 0;
		int length= line.length() - trim;

		while (line.startsWith(trimmable, offset))
			offset += trim;

		while (line.startsWith(trimmable, length))
			length -= trim;

		return new Region(offset, length + trim);
	}
}
