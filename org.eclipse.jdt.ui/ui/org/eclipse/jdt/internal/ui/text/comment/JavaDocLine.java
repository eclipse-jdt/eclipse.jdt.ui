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
public class JavaDocLine extends MultiCommentLine implements IJavaDocAttributes, IHtmlTagConstants, ILinkTagConstants {

	/** Line prefix of javadoc start lines */
	public static final String JAVADOC_START_PREFIX= "/**"; //$NON-NLS-1$

	/** The javadoc attributes of this line */
	private int fAttributes= 0;

	/** The reference indentation of this line */
	private String fReference= ""; //$NON-NLS-1$

	/**
	 * Creates a new javadoc line.
	 * 
	 * @param region Comment region to create the line for
	 */
	protected JavaDocLine(final CommentRegion region) {
		super(region);
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentLine#adaptLine(org.eclipse.jdt.internal.ui.text.comment.CommentLine)
	 */
	protected void adapt(final CommentLine previous) {

		if (!hasAttribute(JAVADOC_ROOT) && !hasAttribute(JAVADOC_PARAMETER))
			fReference= ((JavaDocLine)previous).getReference();
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentLine#append(org.eclipse.jdt.internal.ui.text.comment.CommentRange)
	 */
	protected void append(final CommentRange range) {

		final JavaDocRange current= (JavaDocRange)range;
		final JavaDocRegion parent= (JavaDocRegion)getParent();

		if (current.hasAttribute(JAVADOC_PARAMETER))
			setAttribute(JAVADOC_PARAMETER);
		else if (current.hasAttribute(JAVADOC_ROOT))
			setAttribute(JAVADOC_ROOT);

		final int ranges= getSize();
		if (ranges == 1) {

			final CommentRange first= getFirst();
			final String common= getParent().getText(first.getOffset(), first.getLength()) + CommentRegion.COMMENT_RANGE_DELIMITER;

			if (hasAttribute(JAVADOC_ROOT))
				fReference= common;
			else if (hasAttribute(JAVADOC_PARAMETER)) {
				if (parent.isIndentRootDescriptions())
					fReference= common + "   "; //$NON-NLS-1$
				else
					fReference= common;
			}
		}
		super.append(range);
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentLine#applyStart(org.eclipse.jdt.internal.ui.text.comment.CommentRange, java.lang.String, int)
	 */
	protected void applyStart(final CommentRange range, final String indentation, final int length) {

		final CommentRegion parent= getParent();

		if (parent.getSize() == 1) {
			parent.applyText(getStartingPrefix() + " ", 0, range.getOffset() - parent.getOffset()); //$NON-NLS-1$
		} else
			super.applyStart(range, indentation, length);
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentLine#getReference()
	 */
	protected String getReference() {
		return fReference;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentLine#getStartingPrefix()
	 */
	protected String getStartingPrefix() {
		return JAVADOC_START_PREFIX;
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
	 * Set the attribute <code>attribute</code> to true.
	 * 
	 * @param attribute The attribute to set. Must be a javadoc attribute of <code>JavaDocRange</code>.
	 */
	protected final void setAttribute(final int attribute) {
		fAttributes |= attribute;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentLine#tokenizeLine()
	 */
	protected void tokenizeLine() {

		int offset= 0;
		int index= offset;

		final CommentRegion parent= getParent();
		final CommentRange range= getFirst();
		final int begin= range.getOffset();

		final String content= parent.getText(begin, range.getLength());
		final int length= content.length();

		while (offset < length) {

			while (offset < length && Character.isWhitespace(content.charAt(offset)))
				offset++;

			index= offset;
			if (index < length) {

				if (content.charAt(index) == HTML_TAG_PREFIX) {

					while (index < length && content.charAt(index) != HTML_TAG_POSTFIX)
						index++;

					if (index < length && content.charAt(index) == HTML_TAG_POSTFIX)
						index++;

				} else if (content.startsWith(LINK_TAG_START, index)) {

					while (index < length && content.charAt(index) != LINK_TAG_POSTFIX)
						index++;

					if (index < length && content.charAt(index) == LINK_TAG_POSTFIX)
						index++;

				} else {

					while (index < length && !Character.isWhitespace(content.charAt(index)) && content.charAt(index) != HTML_TAG_PREFIX && content.charAt(index) != LINK_TAG_PREFIX)
						index++;
				}
			}

			if (index - offset > 0) {
				parent.append(CommentObjectFactory.createRange(parent, begin + offset, index - offset));

				offset= index;
			}
		}
	}
}
