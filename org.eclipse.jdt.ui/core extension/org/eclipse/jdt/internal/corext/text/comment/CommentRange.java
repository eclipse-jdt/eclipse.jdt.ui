/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.corext.text.comment;

import org.eclipse.jface.text.Position;

/**
 * Range in a comment region in comment region coordinates.
 * 
 * @since 3.0
 */
public class CommentRange extends Position implements ICommentAttributes, IHtmlTagDelimiters {

	/** The attributes of this range */
	private int fAttributes= 0;

	/**
	 * Creates a new comment range.
	 * 
	 * @param position offset of the range
	 * @param count length of the range
	 */
	public CommentRange(final int position, final int count) {
		super(position, count);
	}

	/**
	 * Is the attribute <code>attribute</code> true?
	 * 
	 * @param attribute the attribute to get
	 * @return <code>true</code> iff this attribute is <code>true</code>,
	 *         <code>false</code> otherwise
	 */
	protected final boolean hasAttribute(final int attribute) {
		return (fAttributes & attribute) == attribute;
	}

	/**
	 * Does this comment range contain a closing HTML tag?
	 * 
	 * @param token token belonging to the comment range
	 * @param tag the HTML tag to check
	 * @return <code>true</code> iff this comment range contains a closing
	 *         html tag, <code>false</code> otherwise
	 */
	protected final boolean isClosingTag(final String token, final String tag) {

		boolean result= token.startsWith(HTML_CLOSE_PREFIX) && token.charAt(token.length() - 1) == HTML_TAG_POSTFIX;
		if (result) {

			setAttribute(COMMENT_CLOSE);
			result= token.substring(HTML_CLOSE_PREFIX.length(), token.length() - 1).equals(tag);
		}
		return result;
	}

	/**
	 * Does this comment range contain an opening HTML tag?
	 * 
	 * @param token token belonging to the comment range
	 * @param tag the HTML tag to check
	 * @return <code>true</code> iff this comment range contains an
	 *         opening html tag, <code>false</code> otherwise
	 */
	protected final boolean isOpeningTag(final String token, final String tag) {

		boolean result= token.length() > 0 && token.charAt(0) == HTML_TAG_PREFIX && !token.startsWith(HTML_CLOSE_PREFIX) && token.charAt(token.length() - 1) == HTML_TAG_POSTFIX;
		if (result) {

			setAttribute(COMMENT_OPEN);
			result= token.startsWith(tag, 1);
		}
		return result;
	}

	/**
	 * Mark the comment range with the occurred HTML tags.
	 * 
	 * @param tags the HTML tags to test for their occurrence
	 * @param token token belonging to the comment range
	 * @param attribute attribute to set if a HTML tag is present
	 * @param open <code>true</code> iff opening tags should be marked,
	 *                <code>false</code> otherwise
	 * @param close <code>true</code> iff closing tags should be marked,
	 *                <code>false</code> otherwise
	 */
	protected final void markHtmlTag(final String[] tags, final String token, final int attribute, final boolean open, final boolean close) {

		if (token.charAt(0) == HTML_TAG_PREFIX && token.charAt(token.length() - 1) == HTML_TAG_POSTFIX) {

			String tag= null;
			boolean isOpen= false;
			boolean isClose= false;

			for (int index= 0; index < tags.length; index++) {

				tag= tags[index];

				isOpen= isOpeningTag(token, tag);
				isClose= isClosingTag(token, tag);

				if ((open && isOpen) || (close && isClose)) {

					setAttribute(attribute);
					break;
				}
			}
		}
	}

	/**
	 * Mark the comment range with the occurred tags.
	 * 
	 * @param tags the tags to test for their occurrence
	 * @param prefix the prefix which is common to all the tags to test
	 * @param token the token belonging to the comment range
	 * @param attribute attribute to set if a tag is present
	 */
	protected final void markPrefixTag(final String[] tags, final char prefix, final String token, final int attribute) {

		if (token.charAt(0) == prefix) {

			String tag= null;
			for (int index= 0; index < tags.length; index++) {

				tag= tags[index];
				if (token.equals(tag)) {

					setAttribute(attribute);
					break;
				}
			}
		}
	}

	/**
	 * Marks the comment range with the HTML range tag.
	 * 
	 * @param token the token belonging to the comment range
	 * @param tag the HTML tag which confines the HTML range
	 * @param level the nesting level of the current HTML range
	 * @param key the key of the attribute to set if the comment range is in
	 *                the HTML range
	 * @param html <code>true</code> iff the HTML tags in this HTML range
	 *                should be marked too, <code>false</code> otherwise
	 * @return the new nesting level of the HTML range
	 */
	protected final int markTagRange(final String token, final String tag, int level, final int key, final boolean html) {

		if (isOpeningTag(token, tag)) {
			if (level++ > 0)
				setAttribute(key);
		} else if (isClosingTag(token, tag)) {
			if (--level > 0)
				setAttribute(key);
		} else if (level > 0) {
			if (html || !hasAttribute(COMMENT_HTML))
				setAttribute(key);
		}
		return level;
	}

	/**
	 * Moves this comment range.
	 * 
	 * @param delta the delta to move the range
	 */
	public final void move(final int delta) {
		offset += delta;
	}

	/**
	 * Set the attribute <code>attribute</code> to true.
	 * 
	 * @param attribute the attribute to set.
	 */
	protected final void setAttribute(final int attribute) {
		fAttributes |= attribute;
	}

	/**
	 * Trims this comment range at the beginning.
	 * 
	 * @param delta amount to trim the range
	 */
	public final void trimBegin(final int delta) {
		offset += delta;
		length -= delta;
	}

	/**
	 * Trims this comment range at the end.
	 * 
	 * @param delta amount to trim the range
	 */
	public final void trimEnd(final int delta) {
		length += delta;
	}
}
