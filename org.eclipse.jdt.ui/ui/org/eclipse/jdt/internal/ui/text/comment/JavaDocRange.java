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
 * Range in a javadoc comment region in comment region coordinates.
 * <p>
 * Comment ranges are considered reference objects.
 * Their offsets and lengths may change over time.
 * </p>
 * 
 * @since 3.0
 */
public class JavaDocRange extends CommentRange implements IJavaDocAttributes, IHtmlTagConstants {

	/** The javadoc attributes of this range */
	private int fAttributes= 0;

	/**
	 * Creates a new javadoc range.
	 * 
	 * @param offset Offset of the range
	 * @param length Length of the range
	 */
	protected JavaDocRange(final int offset, final int length) {
		super(offset, length);
	}

	/**
	 * Is the attribute <code>attribute</code> true?
	 * 
	 * @param attribute The attribute to get. Must be a javadoc attribute of <code>JavaDocRange</code>.
	 * @return <code>true</code> iff this attribute is <code>true</code>, <code>false</code> otherwise.
	 */
	protected final boolean hasAttribute(final int attribute) {
		return (fAttributes & attribute) == attribute;
	}

	/**
	 * Is this javadoc range a closing html tag?
	 * 
	 * @param token Token belonging to the javadoc range
	 * @param tag Html tag to find
	 * @return <code>true</code> iff this javadoc is a closing html tag, <code>false</code> otherwise
	 */
	protected final boolean isClosingTag(final String token, final String tag) {

		boolean result= token.startsWith(HTML_CLOSE_PREFIX) && token.charAt(token.length() - 1) == HTML_TAG_POSTFIX;
		if (result) {

			setAttribute(JAVADOC_CLOSE);
			result= token.substring(HTML_CLOSE_PREFIX.length(), token.length() - 1).equals(tag);
		}
		return result;
	}

	/**
	 * Is this javadoc range an opening html tag?
	 * 
	 * @param token Token belonging to the javadoc range
	 * @param tag Html tag to find
	 * @return <code>true</code> iff this javadoc is an opening html tag, <code>false</code> otherwise
	 */
	protected final boolean isOpeningTag(final String token, final String tag) {

		boolean result= token.charAt(0) == HTML_TAG_PREFIX && !token.startsWith(HTML_CLOSE_PREFIX) && token.charAt(token.length() - 1) == HTML_TAG_POSTFIX;
		if (result) {

			setAttribute(JAVADOC_OPEN);
			result= token.startsWith(tag, 1);
		}
		return result;
	}

	/**
	 * Marks the javadoc range as having a certain html tag for line wrapping.
	 * 
	 * @param tags Html tags which cause the wrapping of the javadoc range
	 * @param token Token belonging to the javadoc range
	 * @param attribute Attribute to set if the html tag is present
	 * @param open <code>true</code> iff opening tags should be marked, <code>false</code> otherwise.
	 * @param close <code>true</code> iff closing tags should be marked, <code>false</code> otherwise.
	 */
	protected final void markHtmlTag(final String[] tags, final String token, final int attribute, final boolean open, final boolean close) {

		if (token.charAt(0) == HTML_TAG_PREFIX && token.charAt(token.length() - 1) == HTML_TAG_POSTFIX) {

			setAttribute(JAVADOC_HTML);

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
	 * Marks the javadoc range as javadoc tag range if it contains a javadoc tag.
	 * 
	 * @param tags Javadoc tags which begin a new paragraph
	 * @param token Token belonging to the javadoc range
	 * @param attribute Attribute to set if the html tag is present
	 */
	protected final void markJavadocTag(final String[] tags, final String token, final int attribute) {

		if (token.charAt(0) == JAVADOC_TAG_PREFIX) {

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
	 * Marks an attributed range with the indicated attribute.
	 * 
	 * @param token Token belonging to the javadoc range
	 * @param tag The html tag which confines the attributed ranges
	 * @param level Hierarchical depth of layered attributed ranges
	 * @param key The key of the attribute to set when an attributed range has been recognized
	 * @param html <code>true</code> iff html tags in this attributed range should be attributed too, <code>false</code> otherwise
	 * @return The new hierarchical depth of the attributed ranges
	 */
	protected final int markRange(final String token, final String tag, int level, final int key, final boolean html) {

		if (isOpeningTag(token, tag)) {
			if (level++ > 0)
				setAttribute(key);
		} else if (isClosingTag(token, tag)) {
			if (--level > 0)
				setAttribute(key);
		} else if (level > 0) {
			if (html || !hasAttribute(JAVADOC_HTML))
				setAttribute(key);
		}
		return level;
	}

	/**
	 * Set the attribute <code>attribute</code> to true.
	 * 
	 * @param attribute The attribute to set. Must be a javadoc attribute of <code>JavaDocRange</code>.
	 */
	protected final void setAttribute(final int attribute) {
		fAttributes |= attribute;
	}
}
