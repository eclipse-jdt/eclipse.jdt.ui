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

import java.util.LinkedList;

/**
 * General comment line in a comment region.
 * 
 * @since 3.0
 */
public abstract class CommentLine implements IBorderAttributes {

	/** The javadoc attributes of this line */
	private int fAttributes= 0;

	/** The region this comment line belongs to */
	private final CommentRegion fParent;

	/** The sequence of comment ranges in this comment line */
	private final LinkedList fRanges= new LinkedList();

	/**
	 * Creates a new comment line.
	 * 
	 * @param region Comment region to create the line for
	 */
	protected CommentLine(final CommentRegion region) {
		fParent= region;
	}

	/**
	 * Gets the context information from the previous comment line and sets it for the current one.
	 * 
	 * @param previous The previous comment line in the associated comment region
	 */
	protected abstract void adapt(final CommentLine previous);

	/**
	 * Appends the comment range to this comment line.
	 * 
	 * @param range Comment range to append
	 */
	protected void append(final CommentRange range) {
		fRanges.add(range);
	}

	/**
	 * Applies the formatted lower border to the underlying document.
	 * 
	 * @param range Last comment range in the comment region
	 * @param indentation Indenation of the formatted lower border
	 * @param length The maximal length of text in this comment region measured in average character widths
	 */
	protected void applyEnd(final CommentRange range, final String indentation, final int length) {

		final int offset= range.getOffset() + range.getLength();
		final CommentRegion parent= getParent();

		final StringBuffer buffer= new StringBuffer(length);
		final String end= getEndingPrefix();
		final String delimiter= parent.getDelimiter();

		if (parent.isSingleLine() && parent.getSize() == 1)
			buffer.append(end);
		else {

			final String filler= getContentPrefix().trim();

			buffer.append(delimiter);
			buffer.append(indentation);

			if (parent.hasBorder(BORDER_LOWER)) {

				buffer.append(' ');
				for (int character= 0; character < length; character++)
					buffer.append(filler);

				buffer.append(end.trim());

			} else
				buffer.append(end);
		}
		parent.applyText(buffer.toString(), offset, parent.getLength() - offset);
	}

	/**
	 * Applies the formatted comment line to the underlying document.
	 * 
	 * @param predecessor The comment line predecessor of this line
	 * @param last The most recently applied comment range of the previous comment line in the comment region
	 * @param indentation Indentation of the formatted comment line
	 * @param line The index of the comment line in the comment region
	 * @return The first comment range in this comment line
	 */
	protected CommentRange applyLine(final CommentLine predecessor, final CommentRange last, final String indentation, final int line) {

		int offset= 0;
		int length= 0;

		CommentRange next= last;
		CommentRange previous= null;

		final CommentRegion parent= getParent();

		final int stop= fRanges.size() - 1;
		final int end= parent.getSize() - 1;

		for (int index= stop; index >= 0; index--) {

			previous= next;
			next= (CommentRange)fRanges.get(index);

			if (parent.canApply(previous, next)) {

				offset= next.getOffset() + next.getLength();
				length= previous.getOffset() - offset;

				if (index == stop && line != end)
					parent.applyText(parent.getDelimiter(predecessor, this, previous, next, indentation), offset, length);
				else
					parent.applyText(parent.getDelimiter(previous, next), offset, length);
			}
		}
		return next;
	}

	/**
	 * Applies the formatted upper border to the underlying document.
	 * 
	 * @param range First comment range in the comment region
	 * @param indentation Indentation of the formatted upper border
	 * @param length The maximal length of text in this comment region measured in average character widths
	 */
	protected void applyStart(final CommentRange range, final String indentation, final int length) {

		final CommentRegion parent= getParent();

		final StringBuffer buffer= new StringBuffer(length);
		final String start= getStartingPrefix();
		final String content= getContentPrefix();

		if (parent.isSingleLine() && parent.getSize() == 1)
			buffer.append(start);
		else {

			final String trimmed= start.trim();
			final String filler= content.trim();

			buffer.append(trimmed);

			if (parent.hasBorder(BORDER_UPPER)) {

				for (int character= 0; character < length - trimmed.length() + start.length(); character++)
					buffer.append(filler);
			}

			buffer.append(parent.getDelimiter());
			buffer.append(indentation);
			buffer.append(content);
		}
		parent.applyText(buffer.toString(), 0, range.getOffset());
	}

	/**
	 * Returns the line prefix of content lines.
	 * 
	 * @return Line prefix of content lines
	 */
	protected abstract String getContentPrefix();

	/**
	 * Returns the line prefix of end lines.
	 * 
	 * @return Line prefix of end lines
	 */
	protected abstract String getEndingPrefix();

	/**
	 * Returns the first comment range in this comment line.
	 * 
	 * @return The first comment range
	 */
	protected final CommentRange getFirst() {
		return (CommentRange)fRanges.getFirst();
	}

	/**
	 * Returns the last comment range in this comment line.
	 * 
	 * @return The last comment range
	 */
	protected final CommentRange getLast() {
		return (CommentRange)fRanges.getLast();
	}

	/**
	 * Returns the parent comment region of this comment line.
	 * 
	 * @return The parent comment region
	 */
	protected final CommentRegion getParent() {
		return fParent;
	}

	/**
	 * Returns the indentation reference string for this line.
	 * 
	 * @return The indentation reference string for this line
	 */
	protected String getReference() {
		return ""; //$NON-NLS-1$
	}

	/**
	 * Returns the number of comment ranges in this comment line.
	 * 
	 * @return The number of ranges in this line
	 */
	protected final int getSize() {
		return fRanges.size();
	}

	/**
	 * Returns the line prefix of start lines.
	 * 
	 * @return Line prefix of start lines
	 */
	protected abstract String getStartingPrefix();

	/**
	 * Is the attribute <code>attribute</code> true?
	 * 
	 * @param attribute The attribute to get.
	 * @return <code>true</code> iff this attribute is <code>true</code>, <code>false</code> otherwise.
	 */
	protected final boolean hasAttribute(final int attribute) {
		return (fAttributes & attribute) == attribute;
	}

	/**
	 * Scans this line in the comment region.
	 * 
	 * @param line Index of this line in the comment region
	 */
	protected abstract void scanLine(final int line);

	/**
	 * Set the attribute <code>attribute</code> to true.
	 * 
	 * @param attribute The attribute to set.
	 */
	protected final void setAttribute(final int attribute) {
		fAttributes |= attribute;
	}

	/** 
	 * Tokenizes this line into token ranges.
	 * 
	 * @param line Index of this line in the comment region
	 */
	protected void tokenizeLine(final int line) {

		int offset= 0;
		int index= offset;

		final CommentRegion parent= getParent();
		final CommentRange range= (CommentRange)fRanges.get(0);
		final int begin= range.getOffset();

		final String content= parent.getText(begin, range.getLength());
		final int length= content.length();

		while (offset < length) {

			while (offset < length && Character.isWhitespace(content.charAt(offset)))
				offset++;

			index= offset;

			while (index < length && !Character.isWhitespace(content.charAt(index)))
				index++;

			if (index - offset > 0) {
				parent.append(CommentObjectFactory.createRange(parent, begin + offset, index - offset));

				offset= index;
			}
		}
	}
}
