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
public class JavaDocRange extends CommentRange implements IJavaDocAttributes, IJavaDocTagConstants {

	/**
	 * Creates a new javadoc range.
	 * 
	 * @param position Offset of the range
	 * @param count Length of the range
	 */
	protected JavaDocRange(final int position, final int count) {
		super(position, count);
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
			if (html || !hasAttribute(COMMENT_HTML))
				setAttribute(key);
		}
		return level;
	}
}
